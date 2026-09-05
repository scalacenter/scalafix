package scalafix.internal.rule

import java.nio.file.Files
import java.util.zip.ZipFile

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import scala.meta._
import scala.meta.io.AbsolutePath

import metaconfig.Conf
import metaconfig.Configured
import scalafix.internal.config.ScalaVersion
import scalafix.internal.rule.ImportMatcher._
import scalafix.lint.Diagnostic
import scalafix.patch.Patch
import scalafix.v1.AnnotatedType
import scalafix.v1.ApplyTree
import scalafix.v1.ClassSignature
import scalafix.v1.Configuration
import scalafix.v1.FunctionTree
import scalafix.v1.IdTree
import scalafix.v1.MacroExpansionTree
import scalafix.v1.MethodSignature
import scalafix.v1.Rule
import scalafix.v1.RuleName.stringToRuleName
import scalafix.v1.SelectTree
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.SemanticTree
import scalafix.v1.SemanticType
import scalafix.v1.SingleType
import scalafix.v1.SuperType
import scalafix.v1.Symbol
import scalafix.v1.SymbolInformation
import scalafix.v1.ThisType
import scalafix.v1.TypeApplyTree
import scalafix.v1.TypeRef
import scalafix.v1.TypeSignature
import scalafix.v1.ValueSignature
import scalafix.v1.XtensionSeqPatch
import scalafix.v1.XtensionTreeScalafix

class OrganizeImports(
    config: OrganizeImportsConfig,
    // shadows the default implicit always on scope (Dialect.current, matching the runtime Scala version)
    implicit val targetDialect: Dialect = Dialect.current,
    scala3DialectForScala3Paths: Boolean = false,
    classpath: List[AbsolutePath] = Nil
) extends SemanticRule("OrganizeImports") {
  import OrganizeImports._
  import ImportMatcher._

  private lazy val scala3TargetDialect =
    new OrganizeImports(config, dialects.Scala3, classpath = classpath)

  private val classfileCache = TrieMap.empty[String, Boolean]

  /**
   * Whether a class file exists on the compilation classpath, regardless of
   * whether its symbol information can be read (a Scala 3 class file cannot,
   * see [[https://github.com/scalacenter/scalafix/issues/2049 issue #2049]]).
   */
  private def hasClassfile(relativePath: String): Boolean =
    classfileCache.getOrElseUpdate(
      relativePath,
      classpath.exists { entry =>
        val path = entry.toNIO
        if (Files.isDirectory(path))
          Files.isRegularFile(path.resolve(relativePath))
        else if (Files.isRegularFile(path) && path.toString.endsWith(".jar"))
          Try {
            val zip = new ZipFile(path.toFile)
            try zip.getEntry(relativePath) != null
            finally zip.close()
          }.getOrElse(false)
        else false
      }
    )

  private val matchers = buildImportMatchers(config)

  private val wildcardGroupIndex: Int = matchers indexOf *

  def this() = this(OrganizeImportsConfig.default)

  override def description: String = "Organize import statements"

  override def isLinter: Boolean = true

  override def isRewrite: Boolean = true

  override def withConfiguration(cfg: Configuration): Configured[Rule] =
    (cfg.conf match {
      case c: Conf.Obj => c.field("OrganizeImports").map(config.merge)
      case _ => None
    }).getOrElse(Configured.Ok(config))
      .andThen(checkRemoveUnusedConflict(_, cfg.conf))
      .andThen(
        checkScalacOptions(
          _,
          cfg.scalacOptions,
          cfg.scalaVersion,
          cfg.scalacClasspath
        )
      )

  override def fix(implicit doc: SemanticDocument): Patch = {
    val that = (doc.input, scala3DialectForScala3Paths) match {
      case (Input.File(path, _), true)
          if path.toFile.getAbsolutePath.contains("scala-3/") =>
        scala3TargetDialect
      case (Input.VirtualFile(path, _), true) if path.contains("scala-3/") =>
        scala3TargetDialect
      case _ => this
    }
    that.fixWithImplicitDialect
  }

  private def fixWithImplicitDialect(implicit doc: SemanticDocument): Patch = {

    val diagnostics: ArrayBuffer[Diagnostic] = ArrayBuffer.empty[Diagnostic]

    val unusedImporteePositions = new UnusedImporteePositions

    val (globalImports, localImports) = doc.tree match {
      case t: Source => collectImports(t.stats)
      case t: Pkg => collectImports(t.body.stats)
      case _ => (Nil, Nil)
    }

    val globalImportsPatch =
      if (globalImports.isEmpty) Patch.empty
      else
        organizeGlobalImports(unusedImporteePositions, diagnostics)(
          globalImports
        )

    val localImportsPatch =
      if (!config.removeUnused || localImports.isEmpty) Patch.empty
      else removeUnusedImports(unusedImporteePositions)(localImports)

    diagnostics.map(Patch.lint).asPatch + globalImportsPatch + localImportsPatch
  }

  private def organizeGlobalImports(
      unusedImporteePositions: UnusedImporteePositions,
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      imports: Seq[Import]
  )(implicit doc: SemanticDocument): Patch = {
    val (fullyQualifiedImporters, otherImporters) = {
      val noUnusedIterator = imports.iterator
        .flatMap(_.importers)
        .flatMap(removeUnusedImporters(unusedImporteePositions))

      val relativeImporters = new ArrayBuffer[Importer]
      val fullyQualifiedIterator =
        if (config.expandRelative)
          noUnusedIterator.map { i =>
            if (isFullyQualified(diagnostics)(i)) i else expandRelative(i)
          }
        else
          noUnusedIterator.filter { i =>
            val ok = isFullyQualified(diagnostics)(i)
            if (!ok) relativeImporters += i
            ok
          }
      val dedupedIterator = deduplicateImportees(fullyQualifiedIterator)
      val expandedIterator = config.expandWildcardImportThreshold.fold(
        dedupedIterator
      ) { threshold =>
        // Re-run deduplication: an expanded wildcard may surface a name that
        // is also imported explicitly by a sibling importer of the same prefix.
        deduplicateImportees(
          dedupedIterator.map(
            new WildcardImportExpander(
              threshold,
              imports.flatMap(_.importers),
              hasClassfile(_)
            ).apply
          )
        )
      }
      val mergedIterator =
        mergeOrExplodeImporters(diagnostics)(expandedIterator)

      // Moves relative imports (when `config.expandRelative` is false) and
      // explicitly imported implicit names into a separate order preserving
      // group. This group will be appended after all the other groups.
      //
      // See https://github.com/liancheng/scalafix-organize-imports/issues/30
      // for why implicits require special handling.
      val otherImporters = new ArrayBuffer[Importer]
      val noImplicits = partitionImplicits(mergedIterator, otherImporters)
      if (relativeImporters.nonEmpty)
        otherImporters ++=
          mergeOrExplodeImporters(diagnostics)(relativeImporters.iterator)

      (noImplicits, otherImporters)
    }

    // Organizes all the fully-qualified global importers.
    val fullyQualifiedGroups: Seq[ImportGroup] =
      groupImporters(fullyQualifiedImporters)

    val orderPreservingGroup = {
      Option(
        otherImporters sortBy (_.importees.head.pos.start)
      ) filter (_.nonEmpty)
    }

    // Builds a patch that inserts the organized imports.
    val insertionPatch = insertOrganizedImports(
      imports.head.tokens.head,
      fullyQualifiedGroups ++
        orderPreservingGroup.map(ImportGroup(matchers.length, _))
    )

    // Builds a patch that removes all the tokens forming the original imports.
    val removalPatch = Patch.removeTokens {
      val importsWithComments = Seq.newBuilder[Tokens]
      imports.foreach { x =>
        importsWithComments += x.tokens
        x.begComment.foreach(importsWithComments += _.tokens)
        x.endComment.foreach(importsWithComments += _.tokens)
      }
      Tokens.merge(importsWithComments.result(): _*).iterator.flatten
    }

    (insertionPatch + removalPatch).atomic
  }

  private def removeUnusedImports(
      unusedImporteePositions: UnusedImporteePositions
  )(
      imports: Seq[Import]
  ): Patch =
    Patch.fromIterable {
      imports flatMap (_.importers) flatMap { case Importer(_, importees) =>
        val hasUsedWildcard = importees exists { i =>
          i.is[Importee.Wildcard] && !unusedImporteePositions(i)
        }

        importees collect {
          case i @ Importee.Rename(_, to)
              if unusedImporteePositions(i) && hasUsedWildcard =>
            // Unimport the identifier instead of removing the importee since
            // unused renamed may still impact compilation by shadowing an
            // identifier.
            //
            // See https://github.com/scalacenter/scalafix/issues/614
            Patch.replaceTree(to, "_").atomic

          case i if unusedImporteePositions(i) =>
            Patch.removeImportee(i).atomic
        }
      }
    }

  private def removeUnusedImporters(
      unusedImporteePositions: UnusedImporteePositions
  )(
      importer: Importer
  ): Option[Importer] =
    if (!config.removeUnused) Some(importer)
    else {
      val hasUsedWildcard = importer.importees exists { i =>
        i.is[Importee.Wildcard] && !unusedImporteePositions(i)
      }

      var rewritten = false

      val noUnused = importer.importees.flatMap {
        case i @ Importee.Rename(from, _)
            if unusedImporteePositions(i) && hasUsedWildcard =>
          // Unimport the identifier instead of removing the importee since
          // unused renamed may still impact compilation by shadowing an
          // identifier.
          //
          // See https://github.com/scalacenter/scalafix/issues/614
          rewritten = true
          Importee.Unimport(from) :: Nil

        case i if unusedImporteePositions(i) =>
          rewritten = true
          Nil

        case i =>
          i :: Nil
      }

      if (!rewritten) Some(importer)
      else if (noUnused.isEmpty) None
      else Some(importer.copy(importees = noUnused))
    }

  private def partitionImplicits(
      importers: Iterator[Importer],
      implicitImporters: ArrayBuffer[Importer]
  )(implicit doc: SemanticDocument): Seq[Importer] = {
    val noImplicitImporters = Seq.newBuilder[Importer]
    val separateImplicits = config.groupSeparately
      .contains(GroupSeparately.ByNameImplicits)
    val separateGivens = targetDialect.allowGivenUsing &&
      config.groupSeparately.contains(GroupSeparately.ByTypeGivens)
    if (separateImplicits || separateGivens) importers.foreach { importer =>
      val (implicits, noImplicits) = importer.importees.partition {
        case i: Importee.Name =>
          separateImplicits && i.symbol.infoNoThrow.exists(_.isImplicit)
        case _: Importee.Given => separateGivens
        case _ => false
      }
      if (implicits.isEmpty) noImplicitImporters += importer
      else if (noImplicits.isEmpty) implicitImporters += importer
      else {
        implicitImporters += importer.copy(importees = implicits)
        noImplicitImporters += importer.copy(importees = noImplicits)
      }
    }
    else importers.foreach(noImplicitImporters += _)
    noImplicitImporters.result()
  }

  private def isFullyQualified(
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      importer: Importer
  )(implicit doc: SemanticDocument): Boolean = {
    val topQualifier = topQualifierOf(importer.ref)
    val topQualifierSymbol = topQualifier.symbol
    val owner = topQualifierSymbol.owner

    (
      // The owner of the top qualifier is `_root_`, e.g.: `import scala.util`
      owner.isRootPackage ||

      // The top qualifier is a top-level class/trait/object defined under no packages. In this
      // case, Scalameta defines the owner to be the empty package.
      owner.isEmptyPackage ||

      // The top qualifier itself is `_root_`, e.g.: `import _root_.scala.util`
      topQualifier.value == "_root_" ||

      // https://github.com/liancheng/scalafix-organize-imports/issues/64:
      // Sometimes, the symbol of the top qualifier can be missing due to
      // unknown reasons. In this case, we issue a warning and continue
      // processing assuming that the top qualifier is fully-qualified.
      topQualifierSymbol.isNone && {
        diagnostics += ImporterSymbolNotFound(topQualifier)
        true
      }
    )
  }

  private def expandRelative(
      importer: Importer
  )(implicit doc: SemanticDocument): Importer = {

    /**
     * Converts a `Symbol` into a fully-qualified `Term.Ref`.
     *
     * NOTE: The returned `Term.Ref` does NOT contain symbol information since
     * it's not parsed from the source file.
     */
    def toFullyQualifiedRef(symbol: Symbol): Term.Ref = {
      val owner = symbol.owner

      symbol match {
        // When importing names defined within package objects, skip the `package` part for brevity.
        // For instance, with the following definition:
        //
        //   package object foo { val x: Int = ??? }
        //
        // when importing `foo.x`, we prefer "import foo.x" instead of "import foo.`package`.x",
        // which is also valid, but unnecessarily lengthy.
        //
        // See https://github.com/liancheng/scalafix-organize-imports/issues/55.
        case _ if symbol.infoNoThrow exists (_.isPackageObject) =>
          toFullyQualifiedRef(owner)

        // See the comment marked with "issues/64" for the case of `symbol.isNone`
        case _
            if symbol.isNone || owner.isRootPackage || owner.isEmptyPackage =>
          Term.Name(symbol.displayName)

        case _ =>
          Term.Select(toFullyQualifiedRef(owner), Term.Name(symbol.displayName))
      }
    }

    val fullyQualifiedTopQualifier =
      toFullyQualifiedRef(topQualifierOf(importer.ref).symbol)

    importer.copy(
      ref = replaceTopQualifier(importer.ref, fullyQualifiedTopQualifier)
    )
  }

  private def groupImporters(importers: Seq[Importer]): Seq[ImportGroup] =
    importers
      .groupBy(matchImportGroup) // Groups imports by importer prefix.
      .map { case (index, grouped) =>
        ImportGroup(index, organizeImportGroup(grouped))
      }
      .toSeq
      .sortBy(_.index)

  private def deduplicateImportees(
      importers: Iterator[Importer]
  ): Iterator[Importer] = {
    // Scalameta `Tree` nodes do not provide structural equality comparisons, here we pretty-print
    // them and compare the string results.
    val seenImportees = mutable.Set.empty[(String, String)]

    importers flatMap { importer =>
      val ref = treeSyntax(importer.ref)
      importer filterImportees { importee =>
        importee.is[Importee.Wildcard] || importee.is[Importee.GivenAll] ||
        seenImportees.add(treeSyntax(importee) -> ref)
      }
    }
  }

  private def mergeOrExplodeImporters(
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      importers: Iterator[Importer]
  ): Iterator[Importer] =
    locally {
      config.groupedImports match {
        case GroupedImports.Merge =>
          mergeImporters(diagnostics)(importers, aggressive = false)
        case GroupedImports.AggressiveMerge =>
          mergeImporters(diagnostics)(importers, aggressive = true)
        case GroupedImports.Explode =>
          explodeImportees(importers)
        case GroupedImports.Keep =>
          importers
      }
    } map (x => sortImportees(coalesceImportees(x)))

  private def organizeImportGroup(
      importeesSorted: Seq[Importer]
  ): Seq[Importer] = {
    def appendImportees(imps: Iterable[Importee], sb: StringBuilder): Unit = {
      val sblen = sb.length
      imps.foreach { imp =>
        if (sb.length > sblen) sb.append(", ")
        sb.append(treeSyntax(imp))
      }
    }
    type SortFunc = (StringBuilder, Boolean, List[Importee]) => Unit
    def sortSyntax(f: SortFunc)(imp: Importer): String = {
      implicit val sb = new StringBuilder
      sb.append(treeSyntax(imp.ref)).append('.')
      f(sb, imp.isCurlyBraced, imp.importees)
      sb.toString()
    }
    val sortSyntaxFunc: SortFunc = (sb, inBraces, imps) => {
      if (inBraces) sb.append('{')
      appendImportees(imps, sb)
      if (inBraces) sb.append('}')
    }
    val symbolsSortFunc: SortFunc = (sb, inBraces, imps) => {
      if (inBraces) sb.append('\u0002')
      imps match {
        case (_: Importee.Wildcard) :: Nil => sb.append('\u0001')
        case _ => appendImportees(imps, sb)
      }
      if (inBraces) sb.append('\u0002')
    }
    def sortImporters[A: Ordering](f: Importer => A) =
      importeesSorted map (x => x -> f(x)) sortBy (_._2) map (_._1)

    config.importsOrder match {
      case ImportsOrder.Ascii =>
        sortImporters(sortSyntax(sortSyntaxFunc))
      case ImportsOrder.AsciiCaseInsensitive =>
        sortImporters { x =>
          val text = sortSyntax(sortSyntaxFunc)(x)
          (text.toLowerCase, text)
        }
      case ImportsOrder.SymbolsFirst =>
        sortImporters(sortSyntax(symbolsSortFunc))
      case ImportsOrder.Keep =>
        importeesSorted
    }
  }

  private def mergeImporters(
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      importers: Iterator[Importer],
      aggressive: Boolean
  ): Iterator[Importer] =
    importers.toList.groupBy(i => treeSyntax(i.ref)).values.iterator.flatMap {
      case group @ ref :: rest if rest.nonEmpty =>
        val importeeLists = group map (_.importees)
        val hasWildcard = group exists (_.hasWildcard)
        val hasGivenAll = group exists (_.hasGivenAll)

        // Collects the last set of unimports with a wildcard, if any. It cancels all previous
        // unimports. E.g.:
        //
        //   import p.{A => _}
        //   import p.{B => _, _}
        //   import p.{C => _, _}
        //
        // Only `C` is unimported. `A` and `B` are still available.
        //
        // TODO: Shall we issue a warning here as using order-sensitive imports is a bad practice?
        val lastUnimportsWithWildcard = importeeLists.reverse collectFirst {
          case Importees(_, _, unimports @ _ :: _, _, _, Some(_)) => unimports
        }

        val lastUnimportsWithGivenAll = importeeLists.reverse collectFirst {
          case Importees(_, _, unimports @ _ :: _, _, Some(_), _) => unimports
        }

        // Collects all unimports without an accompanying wildcard.
        val unimports = importeeLists.collect {
          case Importees(_, _, unimports, _, None, None) =>
            unimports
        }.flatten

        val (givens, nonGivens) =
          group.flatMap(_.importees).partition(_.is[Importee.Given])

        // Here we assume that a name is renamed at most once within a single source file, which is
        // true in most cases.
        //
        // Note that the IntelliJ IDEA Scala import optimizer does not handle this case properly
        // either. If a name is renamed more than once, it only keeps one of the renames in the
        // result and may break compilation (unless other renames are not actually referenced).
        val renames = nonGivens
          .collect { case rename: Importee.Rename => rename }
          .groupBy(_.name.value)
          .map { case (_, renames) =>
            val head = renames.head
            if (renames.tail.nonEmpty)
              diagnostics += TooManyAliases(head.name, renames)
            head
          }
          .toList

        // Collects distinct explicitly imported names, and filters out those that are also renamed.
        // If an explicitly imported name is also renamed, both the original name and the new name
        // are available. This implies that both of them must be preserved in the merged result, but
        // in two separate import statements (Scala only allows a name to appear in an import at
        // most once). E.g.:
        //
        //   import p.A
        //   import p.{A => A1}
        //   import p.B
        //   import p.{B => B1}
        //
        // The above snippet should be rewritten into:
        //
        //   import p.{A, B}
        //   import p.{A => A1, B => B1}
        val (renamedImportedNames, importedNames) = {
          val renamedNames =
            renames.map { case Importee.Rename(Name(from), _) => from }.toSet

          nonGivens
            .filter(_.is[Importee.Name])
            .groupBy { case Importee.Name(Name(name)) => name }
            .map { case (_, importees) => importees.head }
            .toList
            .partition { case Importee.Name(Name(name)) =>
              renamedNames contains name
            }
        }

        val mergedNonGivens = (hasWildcard, lastUnimportsWithWildcard) match {
          case (true, _) =>
            // A few things to note in this case:
            //
            // 1. Unimports are discarded because they are canceled by the wildcard. E.g.:
            //
            //      import scala.collection.mutable.{Set => _, _}
            //      import scala.collection.mutable._
            //
            //    The above two imports should be merged into:
            //
            //      import scala.collection.mutable._
            //
            // 2. Explicitly imported names can NOT be discarded even though they seem to be covered
            //    by the wildcard, unless groupedImports is set to AggressiveMerge. This is because
            //    explicitly imported names have higher precedence than names imported via a
            //    wildcard. Discarding them may introduce ambiguity in some cases. E.g.:
            //
            //      import scala.collection.immutable._
            //      import scala.collection.mutable._
            //      import scala.collection.mutable.Set
            //
            //      object Main { val s: Set[Int] = ??? }
            //
            //    The type of `Main.s` above is unambiguous because `mutable.Set` is explicitly
            //    imported, and has higher precedence than `immutable.Set`, which is made available
            //    via a wildcard. In this case, the imports should be merged into:
            //
            //      import scala.collection.immutable._
            //      import scala.collection.mutable.{Set, _}
            //
            //    rather than
            //
            //      import scala.collection.immutable._
            //      import scala.collection.mutable._
            //
            //    Otherwise, the type of `Main.s` becomes ambiguous and a compilation error is
            //    introduced.
            //
            // 3. However, the case discussed above is relatively rare in real life. A more common
            //    case is something like:
            //
            //      import scala.collection.Set
            //      import scala.collection._
            //
            //    In this case, we do want to merge them into:
            //
            //      import scala.collection._
            //
            //    rather than
            //
            //      import scala.collection.{Set, _}
            //
            //    To achieve this, users may set `groupedImports` to `AggressiveMerge`. Instead of
            //    being conservative and ensure correctness in all the cases, this option merges
            //    imports aggressively for conciseness.
            //
            // 4. Renames must be moved into a separate import statement to make sure that the
            //    original names made available by the wildcard are still preserved. E.g.:
            //
            //      import p._
            //      import p.{A => A1}
            //
            //    The above imports cannot be merged into
            //
            //      import p.{A => A1, _}
            //
            //    Otherwise, the original name `A` is no longer available.
            if (aggressive) Seq(renames, Importee.Wildcard() :: Nil)
            else Seq(renames, importedNames :+ Importee.Wildcard())

          case (false, Some(lastUnimports)) =>
            // A wildcard must be appended for unimports.
            Seq(
              renamedImportedNames,
              importedNames ++ renames ++ lastUnimports :+ Importee.Wildcard()
            )

          case (false, None) =>
            Seq(renamedImportedNames, importedNames ++ renames ++ unimports)
        }

        /* Adjust the result to add givens imports, these are
         * are the Scala 3 way of importing implicits, which are not imported
         * with the wildcard import.
         */
        val newImporteeListsWithGivens = if (hasGivenAll) {
          if (aggressive) mergedNonGivens :+ List(Importee.GivenAll())
          else mergedNonGivens :+ (givens :+ Importee.GivenAll())
        } else {
          lastUnimportsWithGivenAll match {
            case Some(unimports) =>
              mergedNonGivens :+ (givens ++ unimports :+ Importee.GivenAll())
            case None =>
              mergedNonGivens :+ givens
          }
        }

        preserveOriginalImportersFormatting(
          group,
          newImporteeListsWithGivens,
          ref
        )

      // If this group has only one importer, returns it as is to preserve the original source
      // level formatting.
      // Also prevents exhaustive pattern match warning about Nil, which should never happen.
      case group => group
    }

  private def coalesceImportees(importer: Importer): Importer = {
    val Importees(names, renames, unimports, givens, _, _) = importer.importees

    config.coalesceToWildcardImportThreshold
      .filter(importer.importees.length > _)
      // Skips if there's no `Name`s or `Given`s. `Rename`s and `Unimport`s cannot be coalesced.
      .filterNot(_ => names.isEmpty && givens.isEmpty)
      .map {
        case _ if givens.isEmpty => renames ++ unimports :+ Importee.Wildcard()
        case _ if names.isEmpty => renames ++ unimports :+ Importee.GivenAll()
        case _ =>
          renames ++ unimports :+ Importee.GivenAll() :+ Importee.Wildcard()
      }
      .map(importees => importer.copy(importees = importees))
      .getOrElse(importer)
  }

  private def sortImportees(importer: Importer): Importer = {
    import ImportSelectorsOrder._

    // The Scala language spec allows an import expression to have at most one final wildcard, which
    // can only appears in the last position.
    val (wildcards, others) =
      importer.importees partition (i =>
        i.is[Importee.Wildcard] || i.is[Importee.GivenAll]
      )

    val orderedImportees = config.importSelectorsOrder match {
      case Ascii =>
        Seq(others, wildcards) map (_.sortBy(treeSyntax)) reduce (_ ++ _)
      case SymbolsFirst =>
        Seq(others, wildcards) map sortImporteesSymbolsFirst reduce (_ ++ _)
      case Keep =>
        importer.importees
    }

    // Checks whether importees of the input importer are already sorted. If yes, we should return
    // the original importer to preserve the original source level formatting.
    val alreadySorted =
      config.importSelectorsOrder == Keep ||
        (importer.importees corresponds orderedImportees) { (lhs, rhs) =>
          treeSyntax(lhs) == treeSyntax(rhs)
        }

    if (alreadySorted) importer else importer.copy(importees = orderedImportees)
  }

  /**
   * Returns the index of the group to which the given importer belongs. Each
   * group is represented by an `ImportMatcher`. If multiple `ImporterMatcher`s
   * match the given import, the one matches the longest prefix wins.
   */
  private def matchImportGroup(importer: Importer): Int = {
    val (length, index) = matchers
      .map(_.matches(importer))
      .zipWithIndex
      .maxBy(_._1)
    if (length > 0) index else wildcardGroupIndex
  }

  private def insertOrganizedImports(
      token: Token,
      importGroups: Seq[ImportGroup]
  ): Patch = {
    val prettyPrintedGroups = prettyPrintImportGroups(importGroups)

    // Indices of all blank lines configured in `OrganizeImports.groups`, either automatically or
    // manually.
    val blankLineIndices = matchers.zipWithIndex
      .collect { case (`---`, index) => index }
      .iterator
      .buffered
    def skipOneBlankLineIndex(index: Int): Boolean = {
      val ok = blankLineIndices.headOption.exists(_ <= index)
      if (ok) blankLineIndices.next()
      ok
    }
    def skipBlankLineIndices(index: Int): Unit =
      while (skipOneBlankLineIndex(index)) {}

    prettyPrintedGroups.headOption.foreach { case (index, _) =>
      skipBlankLineIndices(index) // skip leading blanks before first group
    }

    val withBlankLines = prettyPrintedGroups.iterator
      .flatMap { case (index, lines) =>
        val blankIter =
          if (skipOneBlankLineIndex(index - 1)) Iterator.single("")
          else Iterator.empty
        skipBlankLineIndices(index)
        blankIter ++ lines.flatMap(_.linesIterator)
      }

    // Global imports within curly-braced packages must be indented accordingly, e.g.:
    //
    //   package foo {
    //     package bar {
    //       import baz
    //       import qux
    //     }
    //   }
    val indent = " " * token.pos.startColumn
    val sb = new StringBuilder
    withBlankLines.foreach { line =>
      // The first line will be inserted at an already indented position.
      if (sb.nonEmpty) {
        sb.append('\n')
        if (line.nonEmpty) sb.append(indent)
      }
      sb.append(line)
    }

    Patch.addLeft(token, sb.toString())
  }

  private def prettyPrintImportGroups(
      groups: Seq[ImportGroup]
  ): Seq[(Int, Seq[String])] = {
    // within each group, for each Importer, get a list of Imports its Importees come from
    // after re-assembly, an Importer might contain Importees from different Imports
    // NB: to find Imports, need to trace parents of the original parsed tree
    val groupsWithImports = groups.map { ig =>
      ig.index -> ig.imports.map { i1 =>
        val i1o = i1.originalPrototype()
        val i1p = i1o.parent.filter(_.hasComments)
        val i2ps = i1.importees.flatMap { i2 =>
          val i2p = i2.originalPrototype().parent.getOrElse(i1o)
          if (i2p eq i1o) None else i2p.parent.filter(_.hasComments)
        }
        (i1, i1p.fold(i2ps)(_ :: i2ps).distinct)
      }
    }

    // make sure to print each comment only once
    val commentsPrinted = mutable.Set.empty[Tree.Comments]
    groupsWithImports.map { case (index, ig) =>
      val res = Seq.newBuilder[String]

      def appendBegComment(pc: Tree.Comments): Unit =
        if (commentsPrinted.add(pc))
          pc.values.foreach(x => res += x.syntax)

      ig.foreach { case (i, ps) =>
        val sb = new StringBuilder
        def appendEndComment(pc: Tree.Comments): Unit =
          if (commentsPrinted.add(pc))
            pc.values.foreach(x => sb.append(' ').append(x.syntax))

        val single =
          if (i.importees.lengthCompare(1) == 0) i.importees.head else null

        ps.foreach(_.begComment.foreach(appendBegComment))
        i.begComment.foreach(appendBegComment)
        if (single != null) single.begComment.foreach(appendBegComment)
        sb.append("import ").append(treeSyntax(i.ref)).append('.')
        if (single != null) {
          val isCurly = single.isCurlyBraced
          val useOuterSpace = isCurly && single
            .originalPrototype()
            .parent
            .exists(_.hasSpaceInCurly)
          if (isCurly) {
            sb.append('{')
            if (useOuterSpace) sb.append(' ')
          }
          sb.append(treeSyntax(single))
          if (isCurly) {
            if (useOuterSpace) sb.append(' ')
            sb.append('}')
          }
          single.endComment.foreach(appendEndComment)
        } else {
          val lines = i.importees.iterator.map(_.pos.startLine).filter(_ >= 0)
          val isMultiline = lines.hasNext && {
            val line = lines.next()
            lines.exists(_ != line)
          }
          val useOuterSpace = !isMultiline && i.importees
            .flatMap(_.originalPrototype().parent)
            .distinct
            .exists(_.hasSpaceInCurly)
          sb.append('{')
          val sep = if (isMultiline) "\n  " else " "
          if (isMultiline) sb.append(sep)
          else if (useOuterSpace) sb.append(' ')
          val sblen = sb.length
          i.importees.foreach { i2 =>
            if (sb.length > sblen) sb.append(',').append(sep)
            val proto = i2.originalPrototype()
            proto.begComment.foreach(appendBegComment)
            sb.append(treeSyntax(i2))
            proto.endComment.foreach(appendEndComment)
          }
          if (isMultiline) sb.append('\n')
          else if (useOuterSpace) sb.append(' ')
          sb.append('}')
        }
        i.endComment.foreach(appendEndComment)
        ps.foreach(_.endComment.foreach(appendEndComment))
        res += sb.toString()
      }

      index -> res.result()
    }
  }

}

object OrganizeImports {
  private case class ImportGroup(index: Int, imports: collection.Seq[Importer])

  private def checkRemoveUnusedConflict(
      ruleConf: OrganizeImportsConfig,
      conf: Conf
  ): Configured[OrganizeImportsConfig] =
    conf.get[RemoveUnusedConfig]("RemoveUnused") match {
      case Configured.Ok(config) if config.imports =>
        Configured.error(
          "\"RemoveUnused.imports\" and \"OrganizeImports\" should not be used together as they can produce broken code. " +
            "Please disable \"RemoveUnused.imports\" by setting it to false, " +
            "and use \"OrganizeImports.removeUnused\" instead to safely remove unused imports."
        )
      case _ => Configured.ok(ruleConf)
    }

  private def checkScalacOptions(
      conf: OrganizeImportsConfig,
      scalacOptions: List[String],
      scalaVersion: String,
      classpath: List[AbsolutePath]
  ): Configured[Rule] = {
    val hasCompilerSupport =
      Seq("3.0", "3.1", "3.2", "3.3.0", "3.3.1", "3.3.2", "3.3.3")
        .forall(v => !scalaVersion.startsWith(v))

    val hasWarnUnused = hasCompilerSupport && {
      val warnUnusedPrefix = Set("-Wunused", "-Ywarn-unused")
      val warnUnusedString = Set("-Wall", "-Xlint", "-Xlint:unused")
      scalacOptions exists { option =>
        (warnUnusedPrefix exists option.startsWith) || (warnUnusedString contains option)
      }
    }

    val (targetDialect, scala3DialectForScala3Paths) =
      conf.targetDialect match {
        case TargetDialect.Auto =>
          val dialect = ScalaVersion
            .from(scalaVersion)
            .map { scalaVersion =>
              def extractSuffixForScalacOption(prefix: String) = {
                scalacOptions
                  .filter(_.startsWith(prefix))
                  .lastOption
                  .map(_.stripPrefix(prefix))
              }

              // We only lookup the Scala 2 option (Scala 3 is `-source`), as the latest Scala 3
              // dialect is used no matter what the actual minor version is anyway, and as of now,
              // the pretty printer is just more permissive with the latest dialect.
              val sourceScalaVersion =
                extractSuffixForScalacOption("-Xsource:")
                  .map(_.stripSuffix("-cross"))
                  .flatMap(ScalaVersion.from(_).toOption)

              scalaVersion.dialect(sourceScalaVersion)
            }
            .getOrElse(Dialect.current)
          (dialect, false)
        case TargetDialect.Scala2 =>
          (dialects.Scala212, false)
        case TargetDialect.Scala3 =>
          (dialects.Scala3, false)
        case TargetDialect.StandardLayout =>
          (dialects.Scala212, true)
      }

    if (!conf.removeUnused || hasWarnUnused)
      Configured.ok(
        new OrganizeImports(
          conf,
          targetDialect,
          scala3DialectForScala3Paths,
          classpath
        )
      )
    else if (hasCompilerSupport)
      Configured.error(
        "A Scala compiler option is required to use OrganizeImports with"
          + " \"OrganizeImports.removeUnused\" set to true. To fix this"
          + " problem, update your build to add `-Ywarn-unused-import` (2.12)"
          + " or `-Wunused:imports` (2.13 and 3.3.4+)."
      )
    else
      Configured.error(
        "\"OrganizeImports.removeUnused\"" + s"is not supported on $scalaVersion as the compiler is"
          + " not providing enough information. Please upgrade the Scala compiler to 3.3.4 or greater."
          + " Otherwise, run the rule with \"OrganizeImports.removeUnused\" set to false"
          + " to organize imports while keeping potentially unused imports."
      )
  }

  private def buildImportMatchers(
      config: OrganizeImportsConfig
  ): Seq[ImportMatcher] = {
    val withWildcard = {
      val parsed = config.groups map parse
      // The wildcard group should always exist. Appends one at the end if omitted.
      if (parsed contains *) parsed else parsed :+ *
    }

    // Inserts a blank line marker between adjacent import groups when `blankLines` is `Auto`.
    config.blankLines match {
      case BlankLines.Manual => withWildcard
      case BlankLines.Auto => withWildcard.flatMap(_ :: --- :: Nil)
    }
  }

  private def positionOf(importee: Importee): Position =
    importee match {
      case Importee.Rename(from, _) => from.pos
      case _ => importee.pos
    }

  @tailrec private def collectImports(
      stats: List[Stat]
  ): (Seq[Import], Seq[Import]) = stats match {
    case (p: Pkg) :: Nil => collectImports(p.body.stats)
    case _ =>
      val globalImports = Seq.newBuilder[Import]
      val localImports = Seq.newBuilder[Import]
      def collectLocalImports(tree: Tree): Unit =
        tree.traverse { case i: Import => localImports += i }
      val statsiter = stats.iterator
      while (statsiter.hasNext) statsiter.next() match {
        case i: Import => globalImports += i
        case i =>
          collectLocalImports(i)
          while (statsiter.hasNext) collectLocalImports(statsiter.next())
      }
      (globalImports.result(), localImports.result())
  }

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case t: Term.Select => topQualifierOf(t.qual)
      case name: Term.Name => name
    }

  /**
   * Replaces the top-qualifier of the input `term` with a new term
   * `newTopQualifier`.
   */
  private def replaceTopQualifier(
      term: Term,
      newTopQualifier: Term.Ref
  ): Term.Ref =
    term match {
      case _: Term.Name =>
        newTopQualifier
      case t: Term.Select =>
        t.copy(qual = replaceTopQualifier(t.qual, newTopQualifier))
    }

  private def sortImporteesSymbolsFirst(
      importees: List[Importee]
  ): List[Importee] = {
    val symbols = ArrayBuffer.empty[(Importee, String)]
    val lowerCases = ArrayBuffer.empty[(Importee, String)]
    val upperCases = ArrayBuffer.empty[(Importee, String)]

    importees.foreach { i =>
      val syntax = treeSyntax(i)
      val head = syntax.head
      val buf =
        if (head.isLower) lowerCases
        else if (head.isUpper) upperCases
        else symbols
      buf += i -> syntax
    }

    List(symbols, lowerCases, upperCases) flatMap (_ sortBy (_._2) map (_._1))
  }

  private def explodeImportees(
      importers: Iterator[Importer]
  ): Iterator[Importer] =
    importers flatMap {
      case importer @ Importer(_, _ :: Nil) =>
        // If the importer has exactly one importee, returns it as is to preserve the original
        // source level formatting.
        importer :: Nil

      case importer @ Importer(
            _,
            Importees(names, renames, unimports, givens, givenAll, wildcard)
          ) if givenAll.isDefined || wildcard.isDefined =>
        // When a wildcard exists, all renames, unimports, and the wildcard must appear in the same
        // importer, e.g.:
        //
        //   import p.{A => _, B => _, C => D, E, _}
        //
        // should be rewritten into
        //
        //   import p.{A => _, B => _, C => D, _}
        //   import p.E
        val importeesList =
          (names ++ givens).map(
            _ :: Nil
          ) :+ (renames ++ unimports ++ wildcard ++ givenAll)
        preserveOriginalImportersFormatting(
          Seq(importer),
          importeesList,
          importer
        )

      case importer =>
        importer.importees map (i => importer.copy(importees = i :: Nil))
    }

  /**
   * https://github.com/liancheng/scalafix-organize-imports/issues/127: After
   * merging or exploding imports, checks whether there are any input importers
   * left untouched. For those importers, returns the original importer instance
   * to preserve the original source level formatting.
   */
  private def preserveOriginalImportersFormatting(
      importers: Seq[Importer],
      newImporteeLists: Seq[List[Importee]],
      refImporter: Importer
  ) = {
    val importerSyntaxMap = importers.map { i => treeSyntax(i) -> i }.toMap

    newImporteeLists filter (_.nonEmpty) map { importees =>
      val newImporter = refImporter.copy(importees = importees)
      importerSyntaxMap.getOrElse(treeSyntax(newImporter), newImporter)
    }
  }

  /**
   * Categorizes a list of `Importee`s into the following four groups:
   *
   *   - Names, e.g., `Seq`, `Option`, etc.
   *   - Renames, e.g., `{Long => JLong}`, `Duration as D`, etc.
   *   - Unimports, e.g., `{Foo => _}` or `Foo as _`.
   *   - Givens, e.g., `given Foo`.
   *   - GivenAll, i.e., `given`.
   *   - Wildcard, i.e., `_` or `*`.
   */
  object Importees {
    def unapply(importees: Seq[Importee]): Some[
      (
          List[Importee.Name],
          List[Importee.Rename],
          List[Importee.Unimport],
          List[Importee.Given],
          Option[Importee.GivenAll],
          Option[Importee.Wildcard]
      )
    ] = {
      val names = ArrayBuffer.empty[Importee.Name]
      val renames = ArrayBuffer.empty[Importee.Rename]
      val givens = ArrayBuffer.empty[Importee.Given]
      val unimports = ArrayBuffer.empty[Importee.Unimport]
      var maybeWildcard: Option[Importee.Wildcard] = None
      var maybeGivenAll: Option[Importee.GivenAll] = None

      importees foreach {
        case i: Importee.Wildcard => maybeWildcard = Some(i)
        case i: Importee.Unimport => unimports += i
        case i: Importee.Rename => renames += i
        case i: Importee.Name => names += i
        case i: Importee.Given => givens += i
        case i: Importee.GivenAll => maybeGivenAll = Some(i)
      }

      Some(
        (
          names.toList,
          renames.toList,
          unimports.toList,
          givens.toList,
          maybeGivenAll,
          maybeWildcard
        )
      )
    }
  }

  class UnusedImporteePositions(implicit doc: SemanticDocument) {
    private val positions: Seq[Position] =
      doc.diagnostics.toSeq.collect {
        // Scala2 says "Unused import" while Scala3 says "unused import"
        case d if d.message.toLowerCase == "unused import" => d.position
      }

    /** Returns true if the importee was marked as unused by the compiler */
    def apply(importee: Importee): Boolean = {
      // positionOf returns the position of `bar` for `import foo.{bar => baz}`
      // this position matches with the diagnostics from Scala2, but Scala3
      // diagnostics has a position for `bar => baz`, which doesn't match
      // with the return value of `positionOf`.
      // We could adjust the behavior of `positionOf` based on Scala version,
      // but this implementation just checking the unusedImporteePosition
      // includes the importee pos, for simplicity.
      val pos = positionOf(importee)
      positions.exists { unused =>
        unused.start <= pos.start && pos.end <= unused.end
      }
    }
  }

  /**
   * Replaces standalone wildcards with the explicit members of their prefix
   * that are actually used in the document (see [[apply]]). Instantiated once
   * per run: it owns the document-wide usage model and memoizes the scope
   * lookups shared by all importers. `importers` are the document's global
   * importers; `hasClassfile` tells whether a class file exists on the
   * classpath (see [[walkOwners]] and [[resolveByClassfile]]).
   */
  private class WildcardImportExpander(
      threshold: Int,
      importers: Seq[Importer],
      hasClassfile: String => Boolean
  )(implicit doc: SemanticDocument) {

    /**
     * Names that an importee cannot spell without backticks, which the printer
     * does not add: `*` and `_` read as a wildcard, `given` as a `given`
     * selector.
     */
    private val unrenderableNames: Set[String] = Set("*", "_", "given")

    /**
     * Universal supertypes whose members are always in scope without an import.
     */
    private val universalParents: Set[String] =
      Set(
        "scala/Any#",
        "scala/AnyRef#",
        "scala/AnyVal#",
        "scala/Matchable#",
        "scala/Singleton#",
        "java/lang/Object#"
      )

    // Caches, initialized before the usage model below, whose document
    // traversal already hits them through [[isMemberOfType]] and
    // [[isBoundByEnclosingScope]].
    private val ownersCache =
      mutable.HashMap.empty[Symbol, (Set[Symbol], Boolean)]
    private val templateOwnersCache =
      mutable.HashMap.empty[Template, Set[Symbol]]

    /**
     * The global symbols defined in this compilation unit, in both the term and
     * the type namespace (a `case class` defines its companion without a tree
     * node of its own). Used by [[isBoundByEnclosingScope]].
     */
    private val definedHere: Set[Symbol] =
      doc.tree
        .collect { case member: Member => member.name.symbol }
        .iterator
        .filter(_.isGlobal)
        .flatMap { symbol =>
          val value = symbol.value
          if (value.endsWith("#"))
            symbol :: Symbol(value.dropRight(1) + ".") :: Nil
          else if (value.endsWith("."))
            symbol :: Symbol(value.dropRight(1) + "#") :: Nil
          else symbol :: Nil
        }
        .toSet

    /**
     * The prefixes of the global wildcard importers, for
     * [[resolveByClassfile]].
     */
    private val wildcardPrefixes: List[Symbol] =
      importers.iterator
        .filter(_.importees.exists(_.is[Importee.Wildcard]))
        .map(_.ref.symbol)
        .filter(_.isGlobal)
        .toList

    /**
     * Whether the document contains an unqualified reference whose symbol
     * SemanticDB does not record and that could not be recovered (see
     * [[resolveByClassfile]]). Such a reference may depend on any wildcard, so
     * [[apply]] then leaves every wildcard untouched.
     */
    private var hasUnresolvedReference: Boolean = false

    /**
     * The wildcard-import dependencies of the document:
     *   - `usedByOwner`: a map from owner to the symbols used *through a
     *     wildcard* (names used unqualified, and `implicit`/`given` members
     *     resolved through synthetics);
     *   - `unmodeledOwners`: the owners whose membership could not be modeled
     *     precisely;
     *   - `usedOwnersByName`: the owners of the used symbols, by importable
     *     name — the same name resolving to two owners is a shadowing between
     *     scopes that an expansion could turn into an ambiguity (see
     *     [[apply]]);
     *   - `implicitOwners`: the owners of the implicits used through
     *     synthetics.
     *
     * A fully-qualified reference (`p.A`) names its owner explicitly and is
     * excluded, as is a reference bound by an enclosing definition (see
     * [[isBoundByEnclosingScope]]); because each reference resolves to the
     * symbol the compiler actually selected, a name covered by a
     * higher-precedence explicit import is attributed to that import, not to a
     * competing wildcard. Selected members that are not ordinary members of
     * their qualifier's type — extension methods, or unresolvable qualifiers —
     * populate `unmodeledOwners` (see [[collectReference]]). Owners are
     * normalized by [[normalizeOwner]].
     */
    private val (
      usedByOwner,
      unmodeledOwners,
      usedOwnersByName,
      implicitOwners
    ) = {
      val used = mutable.LinkedHashSet.empty[Symbol]
      val unmodeled = mutable.HashSet.empty[Symbol]
      val implicits = mutable.HashSet.empty[Symbol]
      doc.tree.traverse {
        case name: Term.Name => collectReference(name, used, unmodeled)
        case name: Type.Name => collectReference(name, used, unmodeled)
      }
      doc.synthetics.foreach(collectImplicitSymbols(_, used, implicits))
      val globals = used.iterator.filter(_.isGlobal).toList
      val byOwner = globals
        .groupBy(symbol => normalizeOwner(symbol.owner))
        .map { case (owner, symbols) => owner -> symbols.toSet }
      val byName = globals
        .flatMap(symbol =>
          importableName(symbol).map(_ -> normalizeOwner(symbol.owner))
        )
        .groupBy(_._1)
        .map { case (name, owners) => name -> owners.map(_._2).toSet }
      (byOwner, unmodeled.toSet, byName, implicits.toSet)
    }

    /**
     * Adds the symbol `name` refers to when the reference depends on a wildcard
     * import. Unqualified names and the head of a selection do, unless they are
     * bound by an enclosing definition (see [[isBoundByEnclosingScope]]). A
     * selected member `qualifier.member` does *not* when it is reached directly
     * through its owner (`p.A` — a fully-qualified reference), but it does when
     * the qualifier is unrelated to the owner: an extension method or other
     * member brought into scope from elsewhere (`receiver.ext`), whose owner is
     * the import scope, not the receiver's type. A definition's own name is
     * skipped (so a same-package wildcard does not report names merely defined
     * in the file); import selectors are `Importee.Name` nodes, never
     * `Term.Name`/`Type.Name`, so never match.
     */
    private def collectReference(
        name: Name,
        used: mutable.Set[Symbol],
        unmodeled: mutable.Set[Symbol]
    ): Unit =
      name.parent match {
        case Some(select: Term.Select) if select.name eq name =>
          classifySelectedMember(select.qual, name.symbol, unmodeled)
        // `Type.Select` is a term-prefixed type path (`a.B`); its `qual` is a
        // `Term.Ref`, so it is handled like a term selection. `Type.Project`
        // (`A#B`) instead has a `Type` qualifier and is always a direct access.
        case Some(select: Type.Select) if select.name eq name =>
          classifySelectedMember(select.qual, name.symbol, unmodeled)
        case Some(project: Type.Project) if project.name eq name => ()
        // An infix or prefix operator call is a selection on its operand.
        case Some(infix: Term.ApplyInfix) if infix.op eq name =>
          classifySelectedMember(infix.lhs, name.symbol, unmodeled)
        case Some(unary: Term.ApplyUnary) if unary.op eq name =>
          classifySelectedMember(unary.arg, name.symbol, unmodeled)
        case Some(member: Member) if member.name eq name => ()
        case _ =>
          val symbol = name.symbol
          if (symbol.isNone) {
            val recovered = resolveByClassfile(name)
            if (recovered.isEmpty) hasUnresolvedReference = true
            else used ++= recovered.filterNot(isBoundByEnclosingScope(name, _))
          } else if (symbol.isGlobal && !isBoundByEnclosingScope(name, symbol))
            used += symbol
      }

    /**
     * Recovers the class a `new C(..)` / `extends C(..)` names when SemanticDB
     * has no occurrence for it — Scala 3 omits it when the class's type
     * arguments are inferred — by looking `C`'s class file up under each
     * wildcard prefix on the classpath. Returns the matching class symbols
     * (normally one), or nothing when the reference cannot be recovered.
     */
    private def resolveByClassfile(name: Name): List[Symbol] =
      name.parent match {
        case Some(_: Init) =>
          wildcardPrefixes.flatMap { prefix =>
            classfilePrefix(prefix)
              .filter(dir => hasClassfile(dir + name.value + ".class"))
              .map(_ => Symbol(prefix.value + name.value + "#"))
          }
        case _ => Nil
      }

    /**
     * The class file path prefix of the members of a package (`p/`) or of a
     * (possibly nested) object (`p/O$`); `None` for any other kind of prefix.
     */
    private def classfilePrefix(symbol: Symbol): Option[String] = {
      val value = symbol.value
      if (symbol.isNone) None
      else if (value.endsWith("/")) Some(value)
      else if (value.endsWith("."))
        classfilePrefix(symbol.owner).map(_ + symbol.displayName + "$")
      else None
    }

    /**
     * Whether the reference to `symbol` at `name` is bound by an enclosing
     * definition rather than by an import: an own or inherited member of an
     * enclosing template (members of its self type included), or a member of an
     * enclosing package clause that is defined in this compilation unit. Such a
     * binding takes precedence over any import, so the reference neither needs
     * the wildcard nor would use an explicit import replacing it — which the
     * compiler would then report as unused. A member of an enclosing package
     * clause defined in *another* compilation unit has lower precedence than an
     * import and does depend on the wildcard.
     */
    private def isBoundByEnclosingScope(name: Name, symbol: Symbol): Boolean = {
      val owner = normalizeOwner(symbol.owner)
      @tailrec def loop(tree: Option[Tree]): Boolean = tree match {
        case None => false
        case Some(template: Template) =>
          templateOwners(template).contains(owner) || loop(template.parent)
        case Some(pkg: Pkg) =>
          (definedHere(symbol) && packageOwners(pkg.symbol).contains(owner)) ||
          loop(pkg.parent)
        // `package object p` is nested in package `p`; its template is handled
        // by the `Template` case above.
        case Some(pkg: Pkg.Object) =>
          (definedHere(symbol) &&
            packageOwners(pkg.symbol.owner).contains(owner)) ||
          loop(pkg.parent)
        case Some(other) => loop(other.parent)
      }
      loop(name.parent)
    }

    /**
     * The owners whose members are in scope inside `template` by definition:
     * the defined class/object (if any), the parents it extends and the
     * declared self type, each with all of its resolvable ancestors. Memoized
     * per template.
     */
    private def templateOwners(template: Template): Set[Symbol] =
      templateOwnersCache.getOrElseUpdate(
        template, {
          val definition = template.parent.collect { case m: Member =>
            m.symbol
          }
          val parents = template.inits.map(_.symbol)
          val selfTypes =
            template.body.selfOpt.flatMap(_.decltpe).toList.flatMap(typeSymbols)
          (definition.toList ++ parents ++ selfTypes).iterator
            .flatMap(walkOwners(_)._1)
            .toSet
        }
      )

    /**
     * The owners whose members a package clause `package p` brings into scope:
     * the package itself and, through its package object, that object's
     * ancestors.
     */
    private def packageOwners(pkg: Symbol): Set[Symbol] = walkOwners(pkg)._1

    /** The nominal symbols composing a (possibly compound) type. */
    private def typeSymbols(tpe: Type): List[Symbol] = tpe match {
      case t: Type.With => typeSymbols(t.lhs) ++ typeSymbols(t.rhs)
      case t: Type.ApplyInfix if t.op.value == "&" =>
        typeSymbols(t.lhs) ++ typeSymbols(t.rhs)
      case t: Type.Refine => t.tpe.toList.flatMap(typeSymbols)
      case t: Type.Annotate => typeSymbols(t.tpe)
      case t: Type.Apply => typeSymbols(t.tpe)
      case t: Type.Name => t.symbol :: Nil
      case t: Type.Select => t.symbol :: Nil
      case t: Type.Project => t.symbol :: Nil
      case _ => Nil
    }

    /**
     * A selected member that is an ordinary (possibly inherited) member of the
     * qualifier's type does not depend on a wildcard and is ignored. Otherwise
     * — an extension method, or a selection whose qualifier type cannot be
     * resolved — its owner is recorded so [[apply]] leaves any wildcard
     * exposing it untouched, as it cannot be rendered as a precise explicit
     * import.
     */
    private def classifySelectedMember(
        qualifier: Term,
        member: Symbol,
        unmodeled: mutable.Set[Symbol]
    ): Unit =
      if (member.isGlobal && !isMemberOfType(qualifier.symbol, member))
        unmodeled += normalizeOwner(member.owner)

    /**
     * Whether `member` is an ordinary member of `qualifier`'s type, inherited
     * members included. `false` when the qualifier's type cannot be resolved,
     * so that an unmodelable selection is treated conservatively (as an
     * extension rather than a direct member access).
     */
    private def isMemberOfType(qualifier: Symbol, member: Symbol): Boolean =
      typeSymbolOf(qualifier)
        .flatMap(exposedOwners)
        .exists(_.contains(normalizeOwner(member.owner)))

    /**
     * Collects, from a synthetic tree, only the symbols of `implicit`/`given`
     * members it references, and their owners. Those are what a wildcard can
     * bring into scope *invisibly* (through implicit/given search); a wildcard
     * exposing one of them is never expanded (see [[apply]]). Every other
     * synthetic symbol — an inferred `.apply`, a fully-qualified reference
     * participating in the synthetic, a macro-expansion internal — is either
     * already visible in source (and thus handled by the unqualified scan) or
     * does not require the import at all; collecting it could reintroduce a
     * clash with a higher-precedence explicit import.
     */
    private def collectImplicitSymbols(
        tree: SemanticTree,
        buf: mutable.Set[Symbol],
        owners: mutable.Set[Symbol]
    ): Unit = {
      def add(info: SymbolInformation): Unit =
        if (info.isImplicit || info.isGiven) {
          buf += info.symbol
          owners += normalizeOwner(info.symbol.owner)
        }
      tree match {
        case IdTree(info) => add(info)
        case SelectTree(qualifier, id) =>
          collectImplicitSymbols(qualifier, buf, owners)
          add(id.info)
        case ApplyTree(function, arguments) =>
          collectImplicitSymbols(function, buf, owners)
          arguments.foreach(collectImplicitSymbols(_, buf, owners))
        case TypeApplyTree(function, _) =>
          collectImplicitSymbols(function, buf, owners)
        case FunctionTree(_, body) => collectImplicitSymbols(body, buf, owners)
        case MacroExpansionTree(beforeExpansion, _) =>
          collectImplicitSymbols(beforeExpansion, buf, owners)
        // Original{,Sub}Tree wrap real source already covered by the source
        // scan; LiteralTree / NoTree contribute no symbols.
        case _ => ()
      }
    }

    /**
     * Scala 3 top-level definitions of a package `p` declared in `F.scala` are
     * owned by a synthetic `p/F$package.` object, which `import p.*` exposes
     * exactly like the package itself; such an owner is folded into its package
     * so that top-level members are neither dropped from an expansion nor
     * mistaken for members brought into scope from elsewhere.
     */
    private def normalizeOwner(owner: Symbol): Symbol = {
      val parent = owner.owner
      if (owner.value.endsWith("$package.") && parent.value.endsWith("/"))
        parent
      else owner
    }

    /**
     * The owner symbols whose members `import prefix._` brings into scope: the
     * prefix itself plus all of its resolvable ancestors (universal supertypes
     * excluded, since their members need no import), with a flag telling
     * whether the scope could be modeled precisely — `false` when an ancestor's
     * definition is not on the classpath. For a package this also walks its
     * package object, whose members — including inherited ones — are visible
     * through the package wildcard; a package object that exists but cannot be
     * read makes the scope unmodelable. Memoized per run: the scope lookups of
     * [[isMemberOfType]], [[isBoundByEnclosingScope]] and [[apply]] repeat the
     * same prefixes.
     */
    private def walkOwners(prefix: Symbol): (Set[Symbol], Boolean) =
      ownersCache.getOrElseUpdate(
        prefix, {
          val owners = mutable.LinkedHashSet.empty[Symbol]
          val visited = mutable.HashSet.empty[String]
          // Adds `self` and all of its resolvable, non-universal ancestors to
          // `owners`; returns false if an ancestor's definition cannot be
          // resolved.
          def walk(self: Symbol): Boolean =
            universalParents(self.value) || !visited.add(self.value) || {
              self.infoNoThrow match {
                case Some(info) =>
                  owners += self
                  info.signature match {
                    case ClassSignature(_, parents, _, _) =>
                      parents.forall(headSymbol(_).exists(walk))
                    case _ => true
                  }
                case None =>
                  false // unresolvable ancestor -> cannot model precisely
              }
            }
          val value = prefix.value
          val complete =
            if (value.endsWith("/")) {
              // Package: direct members are owned by the package itself;
              // further members may be declared (or inherited) by a package
              // object, which we must walk.
              owners += prefix
              val packageObject = Symbol(value + "package.")
              packageObject.infoNoThrow match {
                case Some(_) => walk(packageObject)
                // Without readable symbol information, the package is
                // modelable only if it has no package object at all — as
                // opposed to one compiled to TASTy, whose class file exists
                // but cannot be read.
                case None => !hasClassfile(value + "package.class")
              }
            } else walk(prefix)
          (owners.toSet, complete)
        }
      )

    /**
     * The owners exposed by `import prefix._` (see [[walkOwners]]), or `None`
     * when the scope cannot be modeled precisely, so that [[apply]] can leave
     * the wildcard untouched rather than risk dropping a member declared in an
     * unmodeled ancestor.
     */
    private def exposedOwners(prefix: Symbol): Option[Set[Symbol]] = {
      val (owners, complete) = walkOwners(prefix)
      if (complete) Some(owners) else None
    }

    /**
     * The head nominal symbol of a type, discarding type arguments and
     * unwrapping annotations. Returns `None` for non-nominal types (structural,
     * union, …), which callers treat as "cannot resolve".
     */
    private def headSymbol(tpe: SemanticType): Option[Symbol] = tpe match {
      case TypeRef(_, symbol, _) => symbol.asNonEmpty
      case SingleType(_, symbol) => symbol.asNonEmpty
      case ThisType(symbol) => symbol.asNonEmpty
      case SuperType(_, symbol) => symbol.asNonEmpty
      case AnnotatedType(_, t) => headSymbol(t)
      case _ => None
    }

    /**
     * The type symbol of a term: the value type of a `val`/`var`/parameter, the
     * result type of a method, or the symbol itself for an object/class/package
     * used as a value. `None` when it cannot be resolved.
     */
    private def typeSymbolOf(symbol: Symbol): Option[Symbol] =
      symbol.infoNoThrow.flatMap { info =>
        info.signature match {
          case ValueSignature(tpe) => headSymbol(tpe)
          case MethodSignature(_, _, returnType) => headSymbol(returnType)
          case TypeSignature(_, _, upperBound) => headSymbol(upperBound)
          case _: ClassSignature => symbol.asNonEmpty
          case _ if info.isPackage => symbol.asNonEmpty
          case _ => None
        }
      }

    /**
     * Whether `owner` declares an `implicit` or `given` member, as far as it
     * can be known: from its class signature, or — for a package, whose members
     * cannot be enumerated — from the Scala 3 top-level definitions of this
     * compilation unit. Top-level implicits declared in other files of a
     * package cannot be seen at all; the ones actually used are still caught
     * through synthetics (`implicitOwners`).
     */
    private def declaresImplicits(owner: Symbol): Boolean =
      owner.infoNoThrow.exists(_.signature match {
        case ClassSignature(_, _, _, declarations) =>
          declarations.exists(d => d.isImplicit || d.isGiven)
        case _ => false
      }) || (owner.value.endsWith("/") && topLevelImplicitOwners(owner))

    /**
     * The packages whose Scala 3 top-level definitions in this compilation unit
     * include an `implicit` or `given` member.
     */
    private lazy val topLevelImplicitOwners: Set[Symbol] =
      doc.internal.textDocument.symbols.iterator
        .map(info => Symbol(info.symbol))
        .filter(symbol => symbol.isGlobal && !symbol.owner.value.endsWith("/"))
        .filter(symbol => normalizeOwner(symbol.owner) != symbol.owner)
        .filter(_.infoNoThrow.exists(info => info.isImplicit || info.isGiven))
        .map(symbol => normalizeOwner(symbol.owner))
        .toSet

    /**
     * The name under which a used member would appear in an expansion, or
     * `None` for members a wildcard replacement never needs to cover:
     * constructors cannot be imported by name (the class reference itself
     * drives its import), Scala 3 `given`s are not brought into scope by a
     * `*`/`_` wildcard (they require a `given` import, whose selectors
     * [[apply]] preserves), and compiler-internal `<...>` names (`<init>`)
     * cannot be referenced from source. A setter folds into its getter name
     * (`_=` stripped). When the symbol info cannot be resolved the candidate is
     * kept (over-inclusion is the safe failure mode); whether the resulting
     * name is renderable is judged by [[apply]], which fails closed rather than
     * silently dropping it.
     */
    private def importableName(symbol: Symbol): Option[String] = {
      val exempt =
        symbol.infoNoThrow.exists(info => info.isConstructor || info.isGiven) ||
          symbol.displayName.startsWith("<")
      if (exempt) None else Some(symbol.displayName.stripSuffix("_="))
    }

    /**
     * Whether `prefix` is a package whose package object exists but could not
     * be modeled by [[exposedOwners]]. An unresolvable `<pkg>/package.` symbol
     * usually means no package object at all, but on Scala 3 it can also mean
     * one that exists on the classpath yet is unreadable (metacp reads Scala 2
     * pickles, not TASTy). [[walkOwners]] already fails closed when the package
     * object's class file is found on the classpath; this is a fallback for an
     * incomplete classpath, where the document's own references prove
     * existence: a used or unmodeled symbol owned by the package object.
     * Expanding anyway would emit the plain-package members while dropping the
     * package-object ones, breaking compilation.
     */
    private def hasUnreadablePackageObject(
        prefix: Symbol,
        owners: Set[Symbol]
    ): Boolean =
      prefix.value.endsWith("/") && {
        val packageObject = Symbol(prefix.value + "package.")
        !owners.contains(packageObject) &&
        (usedByOwner.contains(packageObject) ||
          unmodeledOwners.contains(packageObject))
      }

    /**
     * Replaces a standalone wildcard with the explicit members of its prefix
     * that are actually used in the document. The importer is left untouched
     * when a reference in the document could not be resolved (see
     * `hasUnresolvedReference`), when the prefix's scope cannot be modeled
     * precisely (see [[exposedOwners]] and [[hasUnreadablePackageObject]]),
     * when the scope exposes a member used through an unmodelable selection (an
     * extension method — see `unmodeledOwners`), when the scope provides
     * implicits (see [[declaresImplicits]] and `implicitOwners`: an implicit
     * found through an import is invisible in source — it may even be summoned
     * inside a macro expansion — and the names of the implicit members of a
     * syntax object are implementation details that vary across Scala
     * versions), when a used name cannot be rendered as an explicit importee (a
     * `$` identifier), when a used name also resolves to a symbol from another
     * scope (a wildcard import nested in a template shadowing the same name:
     * raising this import to an explicit one would make that inner reference
     * ambiguous), when nothing from it is used, or when the resulting importees
     * — pre-existing selectors included — would reach `threshold` (beyond which
     * a wildcard is preferable, and would anyway be re-introduced by
     * `coalesceToWildcardImportThreshold`). Renames, `given` imports and a
     * `given` wildcard are preserved; only the `*`/`_` is replaced.
     */
    def apply(importer: Importer): Importer = {
      if (!importer.hasWildcard || hasUnresolvedReference) importer
      else
        exposedOwners(importer.ref.symbol) match {
          case None =>
            importer // scope can't be modeled precisely -> leave as-is
          case Some(owners) if owners.exists(unmodeledOwners) =>
            importer // exposes a member used through an extension -> leave as-is
          case Some(owners)
              if hasUnreadablePackageObject(importer.ref.symbol, owners) =>
            importer // package object exists but can't be modeled -> leave as-is
          case Some(owners)
              if owners.exists(implicitOwners) ||
                owners.exists(declaresImplicits) =>
            importer // provides implicits -> leave as-is
          case Some(owners) =>
            val Importees(names, renames, _, givens, givenAll, _) =
              importer.importees
            val alreadyImported =
              (names.iterator.map(_.name.value) ++
                renames.iterator.map { case Importee.Rename(from, _) =>
                  from.value
                }).toSet
            val candidates = owners.iterator
              .flatMap(usedByOwner.getOrElse(_, Set.empty[Symbol]))
              .flatMap(importableName(_))
              .filterNot(alreadyImported)
              .toList
            val expanded = candidates.distinct.sorted
            // A used name expansion cannot faithfully render — one containing
            // `$`, where a legal source identifier (`a$b`) cannot be told
            // apart from a compiler-generated name, or one that reads as a
            // wildcard or a `given` selector in an import — makes the
            // wildcard irreplaceable: dropping the name would break
            // compilation.
            val unrenderable = candidates.exists(name =>
              name.isEmpty || name.contains("$") ||
                unrenderableNames(name)
            )
            // A name that elsewhere resolves to a symbol this scope does not
            // expose is shadowed by another (nested) import scope; an
            // explicit import of it here would make that reference ambiguous.
            val shadowed = expanded.exists { name =>
              usedOwnersByName
                .getOrElse(name, Set.empty[Symbol])
                .exists(owner => !owners.contains(owner))
            }
            // The threshold bounds the size of the resulting importee list —
            // pre-existing selectors included — mirroring how
            // `coalesceToWildcardImportThreshold` counts importees, so an
            // expansion below a not-smaller coalesce threshold is never
            // immediately re-coalesced.
            val resultingSize =
              names.length + renames.length + givens.length + givenAll.size +
                expanded.length
            if (
              unrenderable || shadowed || expanded.isEmpty ||
              resultingSize >= threshold
            )
              importer
            else
              importer.copy(importees =
                names ++ renames ++ givens ++
                  expanded
                    .map(name => Importee.Name(Name.Indeterminate(name))) ++
                  givenAll.toList
              )
        }
    }
  }

  implicit private class SymbolExtension(symbol: Symbol) {

    /**
     * HACK: In certain cases, `Symbol#info` may throw `MissingSymbolException`
     * due to some unknown reason. This implicit class adds a safe version of
     * `Symbol#info` to return `None` instead of throw an exception when this
     * happens.
     *
     * See [[https://github.com/scalacenter/scalafix/issues/1123 issue #1123]].
     */
    def infoNoThrow(implicit doc: SemanticDocument): Option[SymbolInformation] =
      Try(symbol.info).toOption.flatten
  }

  @inline
  private def treeSyntax(tree: Tree)(implicit dialect: Dialect): String =
    tree.reprint()

  implicit private class ImporteeExtension(val importee: Importee)
      extends AnyVal {

    /**
     * Checks whether the `Importee` should be curly-braced when pretty-printed.
     */
    def isCurlyBraced(implicit dialect: Dialect): Boolean =
      !dialect.allowAsForImportRename &&
        importee.isAny[Importee.Rename, Importee.Unimport]
  }

  implicit private class ImporterExtension(val importer: Importer)
      extends AnyVal {

    /**
     * Checks whether the `Importer` should be curly-braced when pretty-printed.
     */
    def isCurlyBraced(implicit dialect: Dialect): Boolean =
      importer.importees.lengthCompare(1) != 0 ||
        importer.importees.head.isCurlyBraced

    /**
     * Returns an `Importer` with all the `Importee`s that are selected from the
     * input `Importer` and satisfy a predicate. If all the `Importee`s are
     * selected, the input `Importer` instance is returned to preserve the
     * original source level formatting. If none of the `Importee`s are
     * selected, returns a `None`.
     */
    def filterImportees(f: Importee => Boolean): Option[Importer] = {
      val filtered = importer.importees filter f
      if (filtered.length == importer.importees.length) Some(importer)
      else if (filtered.isEmpty) None
      else Some(importer.copy(importees = filtered))
    }

    /** Returns true if the `Importer` contains a standalone wildcard. */
    def hasWildcard: Boolean =
      importer.importees.exists(_.is[Importee.Wildcard]) &&
        !importer.importees.exists(_.is[Importee.Unimport])

    /** Returns true if the `Importer` contains a standalone given wildcard. */
    def hasGivenAll: Boolean =
      importer.importees.exists(_.is[Importee.GivenAll]) &&
        !importer.importees.exists(_.is[Importee.Unimport])

    def hasSpaceInCurly: Boolean = {
      def lspace: Boolean = {
        val tokens = importer.importees.head.tokens
        val idx = tokens.rskipWideIf(_.is[Token.HTrivia], -1)
        idx < -1 &&
        tokens.getWideOpt(idx).exists(_.is[Token.LeftBrace])
      }
      def rspace: Boolean = {
        val tokens = importer.importees.last.tokens
        val idx = tokens.skipWideIf(_.is[Token.HTrivia], tokens.length)
        idx > tokens.length &&
        tokens.getWideOpt(idx).exists(_.is[Token.RightBrace])
      }
      lspace || rspace
    }

  }

  implicit private class TreeExtension(val tree: Tree) extends AnyVal {
    def hasSpaceInCurly: Boolean = tree match {
      case i: Importer => i.hasSpaceInCurly
      case _ => false
    }
  }

}
