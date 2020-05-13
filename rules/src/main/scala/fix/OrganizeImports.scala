package fix

import scala.annotation.tailrec
import scala.collection.mutable
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

  private val wildcardGroupIndex: Int = importMatchers indexOf WildcardMatcher

  private val unusedImporteePositions: mutable.Set[Position] = mutable.Set.empty[Position]

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
    unusedImporteePositions ++= doc.diagnostics.filter(_.message == "Unused import").map(_.position)

    val (globalImports, localImports) = collectImports(doc.tree)

    val globalImportsPatch =
      if (globalImports.isEmpty) Patch.empty
      else organizeGlobalImports(globalImports)

    val localImportsPatch =
      if (!config.removeUnused || localImports.isEmpty) Patch.empty
      else removeUnused(localImports)

    globalImportsPatch + localImportsPatch
  }

  private def isUnused(importee: Importee): Boolean =
    unusedImporteePositions contains positionOf(importee)

  private def organizeGlobalImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val (implicits, noImplicits) = partitionImplicits(
      for {
        `import` <- imports
        importer <- `import`.importers
        unusedRemoved <- removeUnused(importer).toSeq
      } yield unusedRemoved
    )

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

  private def removeUnused(imports: Seq[Import]): Patch =
    Patch.fromIterable {
      imports flatMap (_.importers) flatMap {
        case Importer(_, importees) =>
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

      val unusedRemoved = importer.importees.flatMap {
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
      else if (unusedRemoved.isEmpty) None
      else Some(importer.copy(importees = unusedRemoved))
    }

  private def partitionImplicits(
    importers: Seq[Importer]
  )(implicit doc: SemanticDocument): (Seq[Importer], Seq[Importer]) = {
    val (implicits, implicitPositions) = importers.flatMap {
      case importer @ Importer(_, importees) =>
        importees
          .filter(_.is[Importee.Name])
          .filter(_.symbol.info.exists(_.isImplicit))
          // Explicitly imported `scala.languageFeature` implicits are special cased and treated as
          // normal imports due to the following reasons:
          //
          // 1. The IntelliJ IDEA Scala import optimizer does not handle the explicitly imported
          //    implicit names case. Moving `scala.languageFeature` to the last order-preserving
          //    import group produces a different result from IntelliJ, which can be annoying for
          //    users who use both IntelliJ and `OrganizeImports`.
          //
          //    See https://github.com/liancheng/scalafix-organize-imports/issues/30 for more
          //    details.
          //
          // 2. Importing `scala.languageFeature` values is almost the only commonly seen cases
          //    where a Scala developer imports an implicit by name explicitly. Yet, there's
          //    practically zero chance that an implicit of the same type can be imported to cause a
          //    conflict, unless the user is intentionally torturing the Scala compiler. Not
          //    treating them as implicits and group them as other normal imports minimizes the
          //    chance of behaving differently from IntelliJ without.
          //
          // 3. The `scala.languageFeature` values are defined as `object`s in Scala 2.12 but
          //    changed to implicit lazy vals in Scala 2.13. Treating them as implicits means that
          //    `OrganizeImports` may produce different results when `OrganizeImports` users upgrade
          //    their code base from Scala 2.12 to 2.13. This introduces an annoying and unnecessary
          //    thing to be taken care of.
          .filter(_.symbol.owner.normalized != "scala.languageFeature.")
          .map(i => importer.copy(importees = i :: Nil) -> i.pos)
    }.unzip

    val noImplicits = importers.flatMap {
      filterImportees(_) { importee =>
        !implicitPositions.contains(importee.pos)
      }.toSeq
    }

    (implicits, noImplicits)
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
    // If the importer originates from the parser, `Tree.toString` returns the original source text
    // being parsed, and therefore preserves the original source level formatting. If the importer
    // does not originates from the parser, `Tree.toString` pretty-prints it using the Scala 2.11
    // dialect, which is good enough for imports.
    val syntax = importer.toString

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

  private def mergeImporters(importers: Seq[Importer]): Seq[Importer] =
    importers.groupBy(_.ref.syntax).values.toSeq.flatMap {
      case importer :: Nil =>
        // If this group has only one importer, returns it as is to preserve the original source
        // level formatting.
        importer :: Nil

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
        val lastUnimportsWildcard = group.reverse map (_.importees) collectFirst {
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

        val importeesList = (hasWildcard, lastUnimportsWildcard) match {
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

  private def explodeImportees(importers: Seq[Importer]): Seq[Importer] =
    importers.flatMap {
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

  // Returns an importer with all the importees selected from the input importer that satisfy a
  // predicate. If all the importees are selected, the input importer instance is returned to
  // preserve the original source level formatting. If none of the importees are selected, returns
  // a `None`.
  private def filterImportees(importer: Importer)(f: Importee => Boolean): Option[Importer] = {
    val filtered = importer.importees filter f
    if (filtered.length == importer.importees.length) Some(importer)
    else if (filtered.isEmpty) None
    else Some(importer.copy(importees = filtered))
  }
}
