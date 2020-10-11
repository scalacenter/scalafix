package fix

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import metaconfig.Configured
import scala.meta.Import
import scala.meta.Importee
import scala.meta.Importer
import scala.meta.Name
import scala.meta.Pkg
import scala.meta.Source
import scala.meta.Stat
import scala.meta.Term
import scala.meta.Tree
import scala.meta.inputs.Position
import scala.meta.tokens.Token
import scalafix.lint.Diagnostic
import scalafix.patch.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.RuleName.stringToRuleName
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.Symbol
import scalafix.v1.SymbolInformation
import scalafix.v1.XtensionTreeScalafix

class OrganizeImports(config: OrganizeImportsConfig) extends SemanticRule("OrganizeImports") {
  import OrganizeImports._

  private val importMatchers = {
    val matchers = config.groups map ImportMatcher.parse
    // The wildcard group should always exist. Append one at the end if omitted.
    if (matchers contains ImportMatcher.Wildcard) matchers else matchers :+ ImportMatcher.Wildcard
  }

  private val wildcardGroupIndex: Int = importMatchers indexOf ImportMatcher.Wildcard

  private val unusedImporteePositions: mutable.Set[Position] = mutable.Set.empty[Position]

  private val diagnostics: ArrayBuffer[Diagnostic] = ArrayBuffer.empty[Diagnostic]

  def this() = this(OrganizeImportsConfig())

  override def isLinter: Boolean = true

  override def isRewrite: Boolean = true

  override def isExperimental: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("OrganizeImports")(OrganizeImportsConfig()) andThen { conf =>
      val hasWarnUnused = {
        val warnUnusedPrefix = Set("-Wunused", "-Ywarn-unused")
        val warnUnusedString = Set("-Xlint", "-Xlint:unused")
        config.scalacOptions exists { option =>
          (warnUnusedPrefix exists option.startsWith) || (warnUnusedString contains option)
        }
      }

      if (!conf.removeUnused || hasWarnUnused)
        Configured.ok(new OrganizeImports(conf))
      else
        Configured.error(
          "The Scala compiler option \"-Ywarn-unused\" is required to use OrganizeImports with"
            + " \"OrganizeImports.removeUnused\" set to true. To fix this problem, update your"
            + " build to use at least one Scala compiler option like -Ywarn-unused-import (2.11 only),"
            + " -Ywarn-unused, -Xlint:unused (2.12.2 or above) or -Wunused (2.13 only)"
        )
    }

  override def fix(implicit doc: SemanticDocument): Patch = {
    unusedImporteePositions ++=
      doc.diagnostics
        .filter(_.message == "Unused import")
        .map(_.position)

    val (globalImports, localImports) = collectImports(doc.tree)

    val globalImportsPatch =
      if (globalImports.isEmpty) Patch.empty
      else organizeGlobalImports(globalImports)

    val localImportsPatch =
      if (!config.removeUnused || localImports.isEmpty) Patch.empty
      else removeUnused(localImports)

    diagnostics.map(Patch.lint).asPatch + globalImportsPatch + localImportsPatch
  }

  private def isUnused(importee: Importee): Boolean =
    unusedImporteePositions contains positionOf(importee)

  private def organizeGlobalImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val noUnused = imports flatMap (_.importers) flatMap (removeUnused(_).toSeq)

    val (implicits, noImplicits) =
      if (!config.groupExplicitlyImportedImplicitsSeparately) (Nil, noUnused)
      else partitionImplicits(noUnused)

    val (fullyQualifiedImporters, relativeImporters) = noImplicits partition isFullyQualified

    // Organizes all the fully-qualified global importers.
    val fullyQualifiedGroups: Seq[Seq[Importer]] = {
      val expanded = if (config.expandRelative) relativeImporters map expandRelative else Nil
      groupImporters(fullyQualifiedImporters ++ expanded)
    }

    // Moves relative imports (when `config.expandRelative` is false) and explicitly imported
    // implicit names into a separate order preserving group. This group will be appended after
    // all the other groups.
    //
    // See https://github.com/liancheng/scalafix-organize-imports/issues/30 for why implicits
    // require special handling.
    val orderPreservingGroup = {
      val relatives = if (config.expandRelative) Nil else relativeImporters
      relatives ++ implicits sortBy (_.importees.head.pos.start)
    }

    // Builds a patch that inserts the organized imports.
    val insertionPatch = prependOrganizedImports(
      imports.head.tokens.head,
      fullyQualifiedGroups :+ orderPreservingGroup filter (_.nonEmpty)
    )

    // Builds a patch that removes all the tokens forming the original imports.
    val removalPatch = Patch.removeTokens(
      doc.tree.tokens.slice(
        imports.head.tokens.start,
        imports.last.tokens.end
      )
    )

    (insertionPatch + removalPatch).atomic
  }

  private def removeUnused(imports: Seq[Import]): Patch =
    Patch.fromIterable {
      imports flatMap (_.importers) flatMap { case Importer(_, importees) =>
        val hasUsedWildcard = importees exists {
          case i: Importee.Wildcard => !isUnused(i)
          case _                    => false
        }

        importees collect {
          case i @ Importee.Rename(_, to) if isUnused(i) && hasUsedWildcard =>
            // Unimport the identifier instead of removing the importee since unused renamed may
            // still impact compilation by shadowing an identifier.
            //
            // See https://github.com/scalacenter/scalafix/issues/614
            Patch.replaceTree(to, "_").atomic

          case i if isUnused(i) =>
            Patch.removeImportee(i).atomic
        }
      }
    }

  private def removeUnused(importer: Importer): Option[Importer] =
    if (!config.removeUnused) Some(importer)
    else {
      val hasUsedWildcard = importer.importees exists {
        case i: Importee.Wildcard => !isUnused(i)
        case _                    => false
      }

      var rewritten = false

      val noUnused = importer.importees.flatMap {
        case i @ Importee.Rename(from, _) if isUnused(i) && hasUsedWildcard =>
          // Unimport the identifier instead of removing the importee since unused renamed may still
          // impact compilation by shadowing an identifier.
          //
          // See https://github.com/scalacenter/scalafix/issues/614
          rewritten = true
          Importee.Unimport(from) :: Nil

        case i if isUnused(i) =>
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
        importees
          .filter(_.is[Importee.Name])
          .filter(name => name.symbol.infoNoThrow exists (_.isImplicit))
          .map(i => importer.copy(importees = i :: Nil) -> i.pos)
    }.unzip

    val noImplicits = importers.flatMap {
      _.filterImportees { importee => !implicitPositions.contains(importee.pos) }.toSeq
    }

    (implicits, noImplicits)
  }

  private def isFullyQualified(importer: Importer)(implicit doc: SemanticDocument): Boolean = {
    val topQualifier = topQualifierOf(importer.ref)
    val topQualifierSymbol = topQualifier.symbol
    val owner = topQualifierSymbol.owner

    (
      // The owner of the top qualifier is `_root_`, e.g.: `import scala.util`
      owner.isRootPackage

      // The top qualifier is a top-level class/trait/object defined under no packages. In this
      // case, Scalameta defines the owner to be the empty package.
      || owner.isEmptyPackage

      // The top qualifier itself is `_root_`, e.g.: `import _root_.scala.util`
      || topQualifier.value == "_root_"

      // Issue #64: Sometimes, the symbol of the top qualifier can be missing due to unknwon reasons
      // (see https://github.com/liancheng/scalafix-organize-imports/issues/64). In this case, we
      // issue a warning and continue processing assuming that the top qualifier is fully-qualified.
      || topQualifierSymbol.isNone && {
        diagnostics += ImporterSymbolNotFound(topQualifier)
        true
      }
    )
  }

  private def expandRelative(importer: Importer)(implicit doc: SemanticDocument): Importer = {

    /**
     * Converts a `Symbol` into a fully-qualified `Term.Ref`.
     *
     * NOTE: The returned `Term.Ref` does NOT contain symbol information since it's not parsed from
     * the source file.
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

        // See the comment marked with "Issue #64" for the case of `symbol.isNone`
        case _ if symbol.isNone || owner.isRootPackage || owner.isEmptyPackage =>
          Term.Name(symbol.displayName)

        case _ =>
          Term.Select(toFullyQualifiedRef(owner), Term.Name(symbol.displayName))
      }
    }

    val fullyQualifiedTopQualifier = toFullyQualifiedRef(topQualifierOf(importer.ref).symbol)
    importer.copy(ref = replaceTopQualifier(importer.ref, fullyQualifiedTopQualifier))
  }

  private def groupImporters(importers: Seq[Importer]): Seq[Seq[Importer]] =
    importers
      .groupBy(matchImportGroup) // Groups imports by importer prefix.
      .toSeq
      .sortBy { case (index, _) => index } // Sorts import groups by group index
      .map {
        // Organize imports within the same group.
        case (_, importers) => organizeImportGroup(deduplicateImportees(importers))
      }

  private def deduplicateImportees(importers: Seq[Importer]): Seq[Importer] = {
    // Scalameta `Tree` nodes do not provide structural equality comparisons, here we pretty-print
    // them and compare the string results.
    val seenImportees = mutable.Set.empty[(String, String)]

    importers flatMap { importer =>
      importer filterImportees { importee =>
        importee.is[Importee.Wildcard] || seenImportees.add(importee.syntax -> importer.ref.syntax)
      }
    }
  }

  private def organizeImportGroup(importers: Seq[Importer]): Seq[Importer] = {
    // Issue #96: For importers with only a single `Importee.Name` importee, if the importee is
    // curly-braced, remove the unneeded curly-braces. For example: `import p.{X}` should be
    // rewritten into `import p.X`.
    val noUnneededBraces = importers map {
      case importer @ Importer(_, Importee.Name(_) :: Nil) =>
        import Token.{Ident, LeftBrace, RightBrace}

        importer.tokens.reverse.toList match {
          // The `.copy()` call erases the source position information from the original importer,
          // so that instead of returning the original source text, the pretty-printer will reformat
          // `importer` without the unneeded curly-braces.
          case RightBrace() :: Ident(_) :: LeftBrace() :: _ => importer.copy()
          case _                                            => importer
        }

      case importer => importer
    }

    val importeesSorted = locally {
      config.groupedImports match {
        case GroupedImports.Merge   => mergeImporters(noUnneededBraces)
        case GroupedImports.Explode => explodeImportees(noUnneededBraces)
        case GroupedImports.Keep    => noUnneededBraces
      }
    } map (coalesceImportees _ andThen sortImportees)

    config.importsOrder match {
      // Issue #84: The Scalameta `Tree` node pretty-printer checks whether the node originates
      // directly from the parser. If yes, the original source code text is returned, and may
      // interfere imports sort order. The `.copy()` call below erases the source position
      // information so that the pretty-printer would actually pretty-print an `Importer` into a
      // single line.
      //
      // See https://github.com/liancheng/scalafix-organize-imports/issues/84 for more details.
      case ImportsOrder.Ascii        => importeesSorted sortBy (_.copy().syntax)
      case ImportsOrder.SymbolsFirst => sortImportersSymbolsFirst(importeesSorted)
      case ImportsOrder.Keep         => importeesSorted
    }
  }

  private def mergeImporters(importers: Seq[Importer]): Seq[Importer] =
    importers.groupBy(_.ref.syntax).values.toSeq.flatMap {
      case importer :: Nil =>
        // If this group has only one importer, returns it as is to preserve the original source
        // level formatting.
        importer :: Nil

      case group @ Importer(ref, _) :: _ =>
        val importeeLists = group map (_.importees)

        val hasWildcard = importeeLists exists {
          case Importees(_, _, Nil, Some(_)) => true
          case _                             => false
        }

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
        val lastUnimportsWithWildcard =
          importeeLists.reverse collectFirst { case Importees(_, _, unimports @ _ :: _, Some(_)) =>
            unimports
          }

        // Collects all unimports without an accompanying wildcard.
        val allUnimports = importeeLists.collect { case Importees(_, _, unimports, None) =>
          unimports
        }.flatten

        val allImportees = group flatMap (_.importees)

        // Here we assume that a name is renamed at most once within a single source file, which is
        // true in most cases.
        //
        // Note that the IntelliJ IDEA Scala import optimizer does not handle this case properly
        // either. If a name is renamed more than once, it only keeps one of the renames in the
        // result and may break compilation (unless other renames are not actually referenced).
        val renames = allImportees
          .filter(_.is[Importee.Rename])
          .map { case rename: Importee.Rename => rename }
          .groupBy(_.name.value)
          .map {
            case (_, rename :: Nil) => rename
            case (_, renames @ (head @ Importee.Rename(from, _)) :: _) =>
              diagnostics += TooManyAliases(from, renames)
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
          val renamedNames = renames.map { case Importee.Rename(Name(from), _) =>
            from
          }.toSet

          allImportees
            .filter(_.is[Importee.Name])
            .groupBy { case Importee.Name(Name(name)) => name }
            .map { case (_, importees) => importees.head }
            .toList
            .partition { case Importee.Name(Name(name)) => renamedNames contains name }
        }

        val wildcard = Importee.Wildcard()

        val importeesList = (hasWildcard, lastUnimportsWithWildcard) match {
          case (true, _) =>
            // A few things to note in this case:
            //
            // 1. Unimports are discarded because they are canceled by the wildcard.
            //
            // 2. Explicitly imported names can NOT be discarded even though they seem to be covered
            //    by the wildcard. This is because explicitly imported names have higher precedence
            //    than names imported via a wildcard. Discarding them may introduce ambiguity in
            //    some cases. E.g.:
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
            // 3. Renames must be moved into a separate import statement to make sure that the
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
            Seq(renames, importedNames :+ wildcard)

          case (false, Some(unimports)) =>
            // A wildcard must be appended for unimports.
            Seq(renamedImportedNames, importedNames ++ renames ++ unimports :+ wildcard)

          case (false, None) =>
            Seq(renamedImportedNames, importedNames ++ renames ++ allUnimports)
        }

        importeesList filter (_.nonEmpty) map (Importer(ref, _))
    }

  private def sortImportersSymbolsFirst(importers: Seq[Importer]): Seq[Importer] =
    importers.sortBy { importer =>
      // See the comment marked with "Issue #84" for why a `.copy()` is needed.
      val syntax = importer.copy().syntax

      importer match {
        case Importer(_, Importee.Wildcard() :: Nil) =>
          syntax.patch(syntax.lastIndexOfSlice("._"), ".\u0001", 2)

        case _ if importer.isCurlyBraced =>
          syntax
            .replaceFirst("[{]", "\u0002")
            .patch(syntax.lastIndexOf("}"), "\u0002", 1)

        case _ => syntax
      }
    }

  private def coalesceImportees(importer: Importer): Importer = {
    val Importees(names, renames, unimports, _) = importer.importees
    if (names.length <= config.coalesceToWildcardImportThreshold) importer
    else importer.copy(importees = renames ++ unimports :+ Importee.Wildcard())
  }

  private def sortImportees(importer: Importer): Importer = {
    import ImportSelectorsOrder._

    // The Scala language spec allows an import expression to have at most one final wildcard, which
    // can only appears in the last position.
    val (wildcard, noWildcard) = importer.importees partition (_.is[Importee.Wildcard])

    val orderedImportees = config.importSelectorsOrder match {
      case Ascii        => noWildcard.sortBy(_.syntax) ++ wildcard
      case SymbolsFirst => sortImporteesSymbolsFirst(noWildcard) ++ wildcard
      case Keep         => importer.importees
    }

    // Checks whether importees of the input importer are already sorted. If yes, we should return
    // the original importer to preserve the original source level formatting.
    val alreadySorted =
      config.importSelectorsOrder == Keep ||
        (importer.importees corresponds orderedImportees) { (lhs, rhs) =>
          lhs.syntax == rhs.syntax
        }

    if (alreadySorted) importer else importer.copy(importees = orderedImportees)
  }

  // Returns the index of the group to which the given importer belongs. Each group is represented
  // by an `ImportMatcher`. If multiple `ImporterMatcher`s match the given import, the one matches
  // the longest prefix wins.
  private def matchImportGroup(importer: Importer): Int = {
    val matchedGroups = importMatchers
      .map(_ matches importer)
      .zipWithIndex
      .filter { case (length, _) => length > 0 }

    if (matchedGroups.isEmpty) wildcardGroupIndex
    else {
      val (_, index) = matchedGroups.maxBy { case (length, _) => length }
      index
    }
  }
}

object OrganizeImports {
  private def positionOf(importee: Importee): Position =
    importee match {
      case Importee.Rename(from, _) => from.pos
      case _                        => importee.pos
    }

  @tailrec private def collectImports(tree: Tree): (Seq[Import], Seq[Import]) = {
    def extractImports(stats: Seq[Stat]): (Seq[Import], Seq[Import]) = {
      val (importStats, otherStats) = stats span (_.is[Import])
      val globalImports = importStats map { case i: Import => i }
      val localImports = otherStats flatMap (_.collect { case i: Import => i })
      (globalImports, localImports)
    }

    tree match {
      case Source(Seq(p: Pkg)) => collectImports(p)
      case Pkg(_, Seq(p: Pkg)) => collectImports(p)
      case Source(stats)       => extractImports(stats)
      case Pkg(_, stats)       => extractImports(stats)
      case _                   => (Nil, Nil)
    }
  }

  private def prettyPrintImportGroup(group: Seq[Importer]): String =
    group
      .map(i => "import " + fixedImporterSyntax(i))
      .mkString("\n")

  /**
   * HACK: The Scalafix pretty-printer decides to add spaces after open and before close braces in
   * imports, i.e., `import a.{ b, c }` instead of `import a.{b, c}`. Unfortunately, this behavior
   * cannot be overriden. This function removes the unwanted spaces as a workaround. In cases where
   * users do want the inserted spaces, Scalafmt should be used after running the `OrganizeImports`
   * rule.
   */
  private def fixedImporterSyntax(importer: Importer): String =
    importer.pos match {
      case pos: Position.Range =>
        // Position found, implies that `importer` was directly parsed from the source code. Returns
        // the original parsed text to preserve the original source level formatting.
        pos.text

      case Position.None =>
        // Position not found, implies that `importer` is derived from certain existing import
        // statement(s). Pretty-prints it.
        val syntax = importer.syntax

        // NOTE: We need to check whether the input importer is curly braced first and then replace
        // the first "{ " and the last " }" if any. Naive string replacement is insufficient, e.g.,
        // a quoted-identifier like "`{ d }`" may cause broken output.
        (importer.isCurlyBraced, syntax lastIndexOfSlice " }") match {
          case (_, -1)       => syntax
          case (true, index) => syntax.patch(index, "}", 2).replaceFirst("\\{ ", "{")
          case _             => syntax
        }
    }

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => topQualifierOf(qualifier)
      case name: Term.Name           => name
    }

  /** Replaces the top-qualifier of the input `term` with a new term `newTopQualifier`. */
  private def replaceTopQualifier(term: Term, newTopQualifier: Term.Ref): Term.Ref =
    term match {
      case _: Term.Name =>
        newTopQualifier
      case Term.Select(qualifier, name) =>
        Term.Select(replaceTopQualifier(qualifier, newTopQualifier), name)
    }

  private def sortImporteesSymbolsFirst(importees: List[Importee]): List[Importee] = {
    val symbols = ArrayBuffer.empty[Importee]
    val lowerCases = ArrayBuffer.empty[Importee]
    val upperCases = ArrayBuffer.empty[Importee]

    importees.foreach {
      case i if i.syntax.head.isLower => lowerCases += i
      case i if i.syntax.head.isUpper => upperCases += i
      case i                          => symbols += i
    }

    List(symbols, lowerCases, upperCases) flatMap (_ sortBy (_.syntax))
  }

  private def explodeImportees(importers: Seq[Importer]): Seq[Importer] =
    importers flatMap {
      case importer @ Importer(_, _ :: Nil) =>
        // If the importer has exactly one importee, returns it as is to preserve the original
        // source level formatting.
        importer :: Nil

      case Importer(ref, Importees(names, renames, unimports, Some(wildcard))) =>
        // When a wildcard exists, all unimports (if any) and the wildcard must appear in the same
        // importer, e.g.:
        //
        //   import p.{A => _, B => _, C => D, E, _}
        //
        // should be rewritten into
        //
        //   import p.{A => _, B => _, _}
        //   import p.{C => D}
        //   import p.E
        val importeesList = (names ++ renames).map(_ :: Nil) :+ (unimports :+ wildcard)
        importeesList filter (_.nonEmpty) map (Importer(ref, _))

      case importer =>
        importer.importees map (i => importer.copy(importees = i :: Nil))
    }

  /**
   * Categorizes a list of `Importee`s into the following four groups:
   *
   *  - Names, e.g., `Seq`, `Option`, etc.
   *  - Renames, e.g., `{Long => JLong}`, `{Duration => D}`, etc.
   *  - Unimports, e.g., `{Foo => _}`.
   *  - Wildcard, i.e., `_`.
   */
  object Importees {
    def unapply(importees: Seq[Importee]): Option[
      (
        List[Importee], // Names
        List[Importee], // Renames
        List[Importee], // Unimports
        Option[Importee] // Wildcard
      )
    ] = {
      var maybeWildcard: Option[Importee] = None
      val unimports = ArrayBuffer.empty[Importee]
      val renames = ArrayBuffer.empty[Importee]
      val names = ArrayBuffer.empty[Importee]

      importees foreach {
        case i: Importee.Wildcard => maybeWildcard = Some(i)
        case i: Importee.Unimport => unimports += i
        case i: Importee.Rename   => renames += i
        case i: Importee.Name     => names += i
      }

      Option((names.toList, renames.toList, unimports.toList, maybeWildcard))
    }
  }

  private def prependOrganizedImports(token: Token, importGroups: Seq[Seq[Importer]]): Patch = {
    // Global imports within curly-braced packages must be indented accordingly, e.g.:
    //
    //   package foo {
    //     package bar {
    //       import baz
    //       import qux
    //     }
    //   }
    val indentedOutput: Iterator[String] =
      importGroups
        .map(prettyPrintImportGroup)
        .mkString("\n\n")
        .linesIterator
        .zipWithIndex
        .map {
          // The first line will be inserted at an already indented position.
          case (line, 0)                 => line
          case (line, _) if line.isEmpty => line
          case (line, _)                 => " " * token.pos.startColumn + line
        }

    Patch.addLeft(token, indentedOutput mkString "\n")
  }

  implicit private class SymbolExtension(symbol: Symbol) {

    /**
     * HACK: In certain cases, `Symbol#info` may throw `MissingSymbolException` due to some unknown
     * reason. This implicit class adds a safe version of `Symbol#info` to return `None` instead of
     * throw an exception when this happens.
     *
     * See [[https://github.com/scalacenter/scalafix/issues/1123 issue #1123]].
     */
    def infoNoThrow(implicit doc: SemanticDocument): Option[SymbolInformation] =
      Try(symbol.info).toOption.flatten
  }

  implicit private class ImporterExtension(importer: Importer) {

    /** Checks whether the `Importer` is curly-braced when pretty-printed. */
    def isCurlyBraced: Boolean =
      importer.importees match {
        case Importees(_, _ :: _, _, _)        => true // At least one rename
        case Importees(_, _, _ :: _, _)        => true // At least one unimport
        case importees if importees.length > 1 => true // More than one importees
        case _                                 => false
      }

    /**
     * Returns an `Importer` with all the `Importee`s selected from the input `Importer` that
     * satisfy a predicate. If all the `Importee`s are selected, the input `Importer` instance is
     * returned to preserve the original source level formatting. If none of the `Importee`s are
     * selected, returns a `None`.
     */
    def filterImportees(f: Importee => Boolean): Option[Importer] = {
      val filtered = importer.importees filter f
      if (filtered.length == importer.importees.length) Some(importer)
      else if (filtered.isEmpty) None
      else Some(importer.copy(importees = filtered))
    }
  }
}
