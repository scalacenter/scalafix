package fix

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
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
import scala.util.matching.Regex

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.Symbol

import scalafix.v1.RuleName.stringToRuleName
import scalafix.v1.XtensionTreeScalafix

class OrganizeImports(config: OrganizeImportsConfig) extends SemanticRule("OrganizeImports") {
  import OrganizeImports._

  private val importMatchers = {
    val matchers = config.groups map {
      case p if p startsWith "re:" => RegexMatcher(new Regex(p stripPrefix "re:"))
      case "*"                     => WildcardMatcher
      case p                       => PlainTextMatcher(p)
    }

    // The wildcard group should always exist. Append one at the end if omitted.
    matchers ++ (List(WildcardMatcher) filterNot matchers.contains)
  }

  private val wildcardGroupIndex = importMatchers indexOf WildcardMatcher

  def this() = this(OrganizeImportsConfig())

  override def isExperimental: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("OrganizeImports")(OrganizeImportsConfig()) andThen { conf =>
      val hasWarnUnused = {
        val warnUnusedPrefix = Set("-Wunused", "-Ywarn-unused")
        config.scalacOptions exists { option => warnUnusedPrefix exists option.startsWith }
      }

      if (hasWarnUnused || !conf.removeUnused)
        Configured.ok(new OrganizeImports(conf))
      else
        Configured.error(
          "The Scala compiler option \"-Ywarn-unused\" is required to use OrganizeImports with"
            + " \"OrganizeImports.removeUnused\" set to true. To fix this problem, update your"
            + " build to use at least one Scala compiler option that starts with -Ywarn-unused"
            + " or -Wunused (2.13 only)"
        )
    }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val globalImports = collectGlobalImports(doc.tree)
    if (globalImports.isEmpty) Patch.empty else organizeImports(globalImports)
  }

  private def organizeImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val (implicits, noImplicits) =
      partitionImplicits(imports flatMap (_.importers) flatMap removeUnused)

    val (fullyQualifiedImporters, relativeImporters) = noImplicits partition isFullyQualified

    // Organizes all the fully-qualified global importers.
    val fullyQualifiedGroups: Seq[Seq[Importer]] = {
      val expanded = if (config.expandRelative) relativeImporters map expandRelative else Nil
      groupImporters(fullyQualifiedImporters ++ expanded)
    }

    // Moves relative imports (when `config.expandRelative` is false) and explicitly imported
    // implicit names into a separate order preserving group. This group will be appended after
    // all the other groups. See [issue #30][1] for why implicits require special handling.
    //
    // [1]: https://github.com/liancheng/scalafix-organize-imports/issues/30
    val orderPreservingGroup = {
      val relatives = if (config.expandRelative) Nil else relativeImporters
      relatives ++ implicits sortBy (_.importees.head.pos.start)
    }

    // Builds a patch that inserts the organized imports.
    val insertionPatch = insertOrganizedImportsBefore(
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

  private def removeUnused(importer: Importer)(implicit doc: SemanticDocument): Seq[Importer] =
    if (!config.removeUnused) importer :: Nil
    else {
      val unusedImports =
        doc.diagnostics
          .filter(_.message == "Unused import")
          .map(_.position)
          .toSet

      def importeePosition(importee: Importee): Position =
        importee match {
          case Importee.Rename(from, _) => from.pos
          case _                        => importee.pos
        }

      val unusedRemoved = importer.importees filterNot { importee =>
        unusedImports contains importeePosition(importee)
      }

      if (unusedRemoved.isEmpty) Nil
      else importer.copy(importees = unusedRemoved) :: Nil
    }

  private def partitionImplicits(
    importers: Seq[Importer]
  )(implicit doc: SemanticDocument): (Seq[Importer], Seq[Importer]) = {
    if (config.intellijCompatible) {
      (Nil, importers)
    } else {
      val (implicits, implicitPositions) = importers.flatMap {
        case importer @ Importer(_, importees) =>
          importees
            .filter(_.is[Importee.Name])
            .filter(_.symbol.info.exists(_.isImplicit))
            .map(i => importer.copy(importees = i :: Nil) -> i.pos)
      }.unzip

      val noImplicits = importers.flatMap {
        case importer @ Importer(_, importees) =>
          val implicitsRemoved = importees.filterNot(i => implicitPositions.contains(i.pos))
          if (implicitsRemoved.isEmpty) Nil else importer.copy(importees = implicitsRemoved) :: Nil
      }

      (implicits, noImplicits)
    }
  }

  private def expandRelative(importer: Importer)(implicit doc: SemanticDocument): Importer = {
    // NOTE: An `Importer.Ref` instance constructed by `toRef` does NOT contain symbol information
    // since it's not parsed from the source file.
    def toRef(symbol: Symbol): Term.Ref = {
      val owner = symbol.owner
      if (owner.isRootPackage || owner.isEmptyPackage) Term.Name(symbol.displayName)
      else Term.Select(toRef(owner), Term.Name(symbol.displayName))
    }

    if (!config.expandRelative || isFullyQualified(importer)) importer
    else importer.copy(ref = toRef(importer.ref.symbol.normalized))
  }

  private def groupImporters(importers: Seq[Importer]): Seq[Seq[Importer]] = {
    val (_, importerGroups) = importers
      .groupBy(matchImportGroup) // Groups imports by importer prefix.
      .mapValues(organizeImportGroup) // Organize imports within the same group.
      .toSeq
      .sortBy { case (index, _) => index } // Sorts import groups by group index
      .unzip

    importerGroups
  }

  private def organizeImportGroup(importers: Seq[Importer]): Seq[Importer] = {
    val importeesSorted = {
      config.groupedImports match {
        case GroupedImports.Merge   => mergeImporters(importers)
        case GroupedImports.Explode => explodeImportees(importers)
        case GroupedImports.Keep    => importers
      }
    } map (coalesceImportees _ andThen sortImportees _)

    config.importsOrder match {
      case ImportsOrder.Ascii        => importeesSorted sortBy (_.syntax)
      case ImportsOrder.SymbolsFirst => sortImportersSymbolsFirst(importeesSorted)
      case ImportsOrder.Keep         => importeesSorted
    }
  }

  private def sortImportersSymbolsFirst(importers: Seq[Importer]): Seq[Importer] =
    importers.sortBy { importer =>
      val syntax = importer.syntax

      importer match {
        case Importer(_, Importee.Wildcard() :: Nil) =>
          syntax.patch(syntax.lastIndexOfSlice("._"), ".\0", 2)

        case _ if isCurlyBraced(importer) =>
          syntax
            .replaceFirst("[{]", "\2")
            .patch(syntax.lastIndexOf("}"), "\2", 1)

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
      case Ascii        => noWildcard.sortBy(_.syntax)
      case SymbolsFirst => sortImporteesSymbolsFirst(noWildcard)
      case Keep         => noWildcard
    }

    importer.copy(importees = orderedImportees ++ wildcard)
  }

  // Returns the index of the group to which the given importer belongs.
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
  @tailrec private def collectGlobalImports(tree: Tree): Seq[Import] = {
    def extractImports(stats: Seq[Stat]): Seq[Import] =
      stats takeWhile (_.is[Import]) collect { case i: Import => i }

    tree match {
      case Source(Seq(p: Pkg)) => collectGlobalImports(p)
      case Pkg(_, Seq(p: Pkg)) => collectGlobalImports(p)
      case Source(stats)       => extractImports(stats)
      case Pkg(_, stats)       => extractImports(stats)
      case _                   => Nil
    }
  }

  private def isFullyQualified(importer: Importer)(implicit doc: SemanticDocument): Boolean = {
    val owner = topQualifierOf(importer.ref).symbol.owner
    owner.isRootPackage || owner.isEmptyPackage
  }

  private def prettyPrintImportGroup(group: Seq[Importer]): String =
    group
      .map { i => "import " + fixedImporterSyntax(i) }
      .mkString("\n")

  // HACK: The scalafix pretty-printer decides to add spaces after open and before close braces in
  // imports, i.e., "import a.{ b, c }" instead of "import a.{b, c}". Unfortunately, this behavior
  // cannot be overriden. This function removes the unwanted spaces as a workaround.
  private def fixedImporterSyntax(importer: Importer): String = {
    val syntax = importer.syntax

    // NOTE: We need to check whether the input importer is curly braced first and then replace the
    // first "{ " and the last " }" if any. Naive string replacements are not sufficient, e.g., a
    // quoted-identifier like "`{ d }`" may cause broken output.
    (isCurlyBraced(importer), syntax lastIndexOfSlice " }") match {
      case (_, -1)       => syntax
      case (true, index) => syntax.patch(index, "}", 2).replaceFirst("\\{ ", "{")
      case _             => syntax
    }
  }

  private def isCurlyBraced(importer: Importer): Boolean =
    importer.importees match {
      case Importees(_, _ :: _, _, _)        => true // At least one rename
      case Importees(_, _, _ :: _, _)        => true // At least one unimport
      case importees if importees.length > 1 => true // Multiple importees
      case _                                 => false
    }

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => topQualifierOf(qualifier)
      case name: Term.Name           => name
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

  private def mergeImporters(importers: Seq[Importer]): Seq[Importer] = {
    importers.groupBy(_.ref.syntax).values.toSeq.flatMap {
      case group @ (Importer(ref, _) :: _) =>
        val hasWildcard = group map (_.importees) exists {
          case Importees(_, _, Nil, Some(_)) => true
          case _                             => false
        }

        // Collects the last set of unimports, which cancels previous unimports. E.g.:
        //
        //   import p.{A => _, B => _, _}
        //   import p.{C => _, _}
        //
        // Only `C` is unimported. `A` and `B` are still available.
        //
        // NOTE: Here we only care about unimports with a wildcard. Unimports without a wildcard is
        // still legal but meaningless. E.g.:
        //
        //   import p.{A => _, _} // Import everything under `p` except `A`.
        //   import p.{A => _}    // Legal, but meaningless.
        val lastUnimports = group.reverse map (_.importees) collectFirst {
          case Importees(_, _, unimports @ (_ :: _), Some(_)) => unimports
        }

        val allImportees = group flatMap (_.importees)

        val renames = allImportees
          .filter(_.is[Importee.Rename])
          .groupBy { case Importee.Rename(Name(name), _) => name }
          .mapValues(_.head)
          .values
          .toList

        // Collects distinct explicitly imported names, and filters out those that are also renamed.
        // If an explicitly imported name is also renamed, both the original name and the new name
        // are available. This implies that both of them must be preserved in the merged result, but
        // in two separate import statements, since Scala disallows a name to appear more than once
        // in a single imporr statement. E.g.:
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
        val (renamedNames, names) = allImportees
          .filter(_.is[Importee.Name])
          .groupBy { case Importee.Name(Name(name)) => name }
          .mapValues(_.head)
          .values
          .toList
          .partition {
            case Importee.Name(Name(name)) =>
              renames exists {
                case Importee.Rename(Name(`name`), _) => true
                case _                                => false
              }
          }

        val importeesList = (hasWildcard, lastUnimports) match {
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
            Seq(renames, names :+ Importee.Wildcard())

          case (false, Some(unimports)) =>
            // A wildcard must be appended for unimports.
            Seq(renamedNames, names ++ renames ++ unimports :+ Importee.Wildcard())

          case (false, None) =>
            Seq(renamedNames, names ++ renames)
        }

        importeesList filter (_.nonEmpty) map (Importer(ref, _))
    }
  }

  private def explodeImportees(importers: Seq[Importer]): Seq[Importer] =
    importers.flatMap {
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

  // An extractor that categorizes a list of `Importee`s into different groups.
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

  private def insertOrganizedImportsBefore(
    token: Token,
    importGroups: Seq[Seq[Importer]]
  ): Patch = {
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
        .lines
        .zipWithIndex
        .map {
          // The first line will be inserted at an already indented position.
          case (line, 0)                 => line
          case (line, _) if line.isEmpty => line
          case (line, _)                 => " " * token.pos.startColumn + line
        }

    Patch.addLeft(token, indentedOutput mkString "\n")
  }
}
