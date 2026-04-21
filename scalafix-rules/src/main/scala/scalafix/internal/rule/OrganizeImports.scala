package scalafix.internal.rule

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import scala.meta._

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfOps
import metaconfig.Configured
import metaconfig.internal.ConfGet
import scalafix.internal.config.ScalaVersion
import scalafix.internal.rule.ImportMatcher._
import scalafix.lint.Diagnostic
import scalafix.patch.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.RuleName.stringToRuleName
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.Symbol
import scalafix.v1.SymbolInformation
import scalafix.v1.XtensionSeqPatch
import scalafix.v1.XtensionTreeScalafix

class OrganizeImports(
    config: OrganizeImportsConfig,
    // shadows the default implicit always on scope (Dialect.current, matching the runtime Scala version)
    implicit val targetDialect: Dialect = Dialect.current,
    scala3DialectForScala3Paths: Boolean = false
) extends SemanticRule("OrganizeImports") {
  import OrganizeImports._
  import ImportMatcher._

  private lazy val scala3TargetDialect =
    new OrganizeImports(config, dialects.Scala3)

  private val matchers = buildImportMatchers(config)

  private val wildcardGroupIndex: Int = matchers indexOf *

  def this() = this(OrganizeImportsConfig())

  override def description: String = "Organize import statements"

  override def isLinter: Boolean = true

  override def isRewrite: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("OrganizeImports")(OrganizeImportsConfig())
      .andThen(patchPreset(_, config.conf))
      .andThen(checkRemoveUnusedConflict(_, config.conf))
      .andThen(checkScalacOptions(_, config.scalacOptions, config.scalaVersion))

  override def fix(implicit doc: SemanticDocument): Patch = {
    (doc.input, scala3DialectForScala3Paths) match {
      case (Input.File(path, _), true)
          if path.toFile.getAbsolutePath.contains("scala-3/") =>
        scala3TargetDialect.fixWithImplicitDialect(doc)
      case (Input.VirtualFile(path, _), true) if path.contains("scala-3/") =>
        scala3TargetDialect.fixWithImplicitDialect(doc)
      case _ =>
        fixWithImplicitDialect(doc)
    }
  }

  private def fixWithImplicitDialect(implicit doc: SemanticDocument): Patch = {

    val diagnostics: ArrayBuffer[Diagnostic] = ArrayBuffer.empty[Diagnostic]

    val unusedImporteePositions = new UnusedImporteePositions

    val (globalImports, localImports) = collectImports(doc.tree)

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
    val noUnused = imports flatMap (_.importers) flatMap (
      removeUnusedImporters(unusedImporteePositions)(_).toSeq
    )

    val (implicits, noImplicits) =
      if (!config.groupExplicitlyImportedImplicitsSeparately) (Nil, noUnused)
      else partitionImplicits(noUnused)

    val (fullyQualifiedImporters, relativeImporters) =
      noImplicits partition isFullyQualified(diagnostics)

    // Organizes all the fully-qualified global importers.
    val fullyQualifiedGroups: Seq[ImportGroup] = {
      val expanded =
        if (config.expandRelative) relativeImporters map expandRelative else Nil
      groupImporters(diagnostics)(fullyQualifiedImporters ++ expanded)
    }

    // Moves relative imports (when `config.expandRelative` is false) and
    // explicitly imported implicit names into a separate order preserving
    // group. This group will be appended after all the other groups.
    //
    // See https://github.com/liancheng/scalafix-organize-imports/issues/30
    // for why implicits require special handling.
    val orderPreservingGroup = {
      val relatives = if (config.expandRelative) Nil else relativeImporters
      Option(
        relatives ++ implicits sortBy (_.importees.head.pos.start)
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
      importers: Seq[Importer]
  )(implicit doc: SemanticDocument): (Seq[Importer], Seq[Importer]) = {
    val (implicits, implicitPositions) = importers.flatMap {
      case importer @ Importer(_, importees) =>
        importees collect {
          case i: Importee.Name if i.symbol.infoNoThrow exists (_.isImplicit) =>
            importer.copy(importees = i :: Nil) -> i.pos
        }
    }.unzip

    val noImplicits = importers.flatMap {
      _.filterImportees { importee =>
        !implicitPositions.contains(importee.pos)
      }.toSeq
    }

    (implicits, noImplicits)
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

  private def groupImporters(
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      importers: Seq[Importer]
  ): Seq[ImportGroup] =
    importers
      .groupBy(matchImportGroup) // Groups imports by importer prefix.
      .map { case (index, grouped) =>
        val organized =
          organizeImportGroup(diagnostics)(deduplicateImportees(grouped))
        ImportGroup(index, organized)
      }
      .toSeq
      .sortBy(_.index)

  private def deduplicateImportees(importers: Seq[Importer]): Seq[Importer] = {
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

  private def organizeImportGroup(
      diagnostics: ArrayBuffer[Diagnostic]
  )(
      importers: Seq[Importer]
  ): Seq[Importer] = {
    val importeesSorted = locally {
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
      importers: Seq[Importer],
      aggressive: Boolean
  ): Seq[Importer] =
    importers.groupBy(i => treeSyntax(i.ref)).values.toSeq.flatMap {
      case importer :: Nil =>
        // If this group has only one importer, returns it as is to preserve the original source
        // level formatting.
        importer :: Nil

      case group @ (ref: Importer) :: _ =>
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
          group.flatMap(_.importees).toList.partition(_.is[Importee.Given])

        // Here we assume that a name is renamed at most once within a single source file, which is
        // true in most cases.
        //
        // Note that the IntelliJ IDEA Scala import optimizer does not handle this case properly
        // either. If a name is renamed more than once, it only keeps one of the renames in the
        // result and may break compilation (unless other renames are not actually referenced).
        val renames = nonGivens
          .collect { case rename: Importee.Rename => rename }
          .groupBy(_.name.value)
          .map { case (_, renames @ head :: rest) =>
            if (rest.nonEmpty)
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
    val matchedGroups = matchers
      .map(_ matches importer)
      .zipWithIndex
      .filter { case (length, _) => length > 0 }

    if (matchedGroups.isEmpty) wildcardGroupIndex
    else {
      val (_, index) = matchedGroups.maxBy { case (length, _) => length }
      index
    }
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

        ps.foreach(_.begComment.foreach(appendBegComment))
        i.begComment.foreach(appendBegComment)
        sb.append("import ").append(treeSyntax(i))
        i.endComment.foreach(appendEndComment)
        ps.foreach(_.endComment.foreach(appendEndComment))
        res += sb.toString()
      }

      index -> res.result()
    }
  }

  implicit private class ImporterExtension(importer: Importer) {

    /**
     * Checks whether the `Importer` should be curly-braced when pretty-printed.
     */
    def isCurlyBraced: Boolean = {
      val importees @ Importees(_, renames, unimports, _, _, _) =
        importer.importees

      importees.length > 1 ||
      ((renames.length == 1 || unimports.length == 1) && !targetDialect.allowAsForImportRename)
    }

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
    def hasWildcard: Boolean = {
      val Importees(_, _, unimports, _, _, wildcard) = importer.importees
      unimports.isEmpty && wildcard.nonEmpty
    }

    /** Returns true if the `Importer` contains a standalone given wildcard. */
    def hasGivenAll: Boolean = {
      val Importees(_, _, unimports, _, givenAll, _) = importer.importees
      unimports.isEmpty && givenAll.nonEmpty
    }
  }
}

object OrganizeImports {
  private case class ImportGroup(index: Int, imports: Seq[Importer])

  private def patchPreset(
      ruleConf: OrganizeImportsConfig,
      conf: Conf
  ): Configured[OrganizeImportsConfig] = {
    val preset = OrganizeImportsConfig.presets(ruleConf.preset)
    val presetConf = ConfEncoder[OrganizeImportsConfig].write(preset)
    val userConf = ConfGet
      .getOrOK(conf, "OrganizeImports" :: Nil, Configured.ok, Conf.Obj.empty)
      .getOrElse(Conf.Obj.empty)
    val mergedConf = ConfOps.merge(presetConf, userConf)
    ConfDecoder[OrganizeImportsConfig].read(mergedConf)
  }

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
      scalaVersion: String
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
        new OrganizeImports(conf, targetDialect, scala3DialectForScala3Paths)
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
      tree: Tree
  ): (Seq[Import], Seq[Import]) = {
    def extractImports(stats: Seq[Stat]): (Seq[Import], Seq[Import]) = {
      val (importStats, otherStats) = stats.span(_.is[Import])
      val globalImports = importStats.map { case i: Import => i }
      val localImports = otherStats.flatMap(_.collect { case i: Import => i })
      (globalImports, localImports)
    }

    tree match {
      case Source(Seq(p: Pkg)) => collectImports(p)
      case Pkg(_, Seq(p: Pkg)) => collectImports(p)
      case Source(stats) => extractImports(stats)
      case Pkg(_, stats) => extractImports(stats)
      case _ => (Nil, Nil)
    }
  }

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => topQualifierOf(qualifier)
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
      case Term.Select(qualifier, name) =>
        Term.Select(replaceTopQualifier(qualifier, newTopQualifier), name)
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

  private def explodeImportees(importers: Seq[Importer]): Seq[Importer] =
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
    def unapply(importees: Seq[Importee]): Option[
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

      Option(
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

}
