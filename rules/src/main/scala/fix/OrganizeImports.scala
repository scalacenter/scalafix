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
import scala.util.matching.Regex

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.RuleName.stringToRuleName
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.Symbol
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
    if (matchers contains WildcardMatcher) matchers else matchers :+ WildcardMatcher
  }

  private val wildcardGroupIndex = importMatchers indexOf WildcardMatcher

  def this() = this(OrganizeImportsConfig())

  override def isExperimental: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("OrganizeImports")(OrganizeImportsConfig()) andThen { conf =>
      val hasWarnUnused = {
        val warnUnusedPrefix = Set("-Wunused", "-Ywarn-unused")
        config.scalacOptions exists { option => warnUnusedPrefix exists (option.startsWith _) }
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

  private def removeUnused(importer: Importer)(implicit doc: SemanticDocument): Seq[Importer] =
    if (!config.removeUnused) importer :: Nil
    else {
      val unusedImports =
        doc.diagnostics
          .filter(_.message == "Unused import")
          .map(_.position)
          .toSet

      def importeePosition(importee: Importee): Position = importee match {
        case Importee.Rename(from, _) => from.pos
        case _                        => importee.pos
      }

      val unusedRemoved = importer.importees filterNot { importee =>
        unusedImports contains importeePosition(importee)
      }

      if (unusedRemoved.isEmpty) Nil
      else importer.copy(importees = unusedRemoved) :: Nil
    }

  private def organizeImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val (fullyQualifiedImporters, relativeImporters) =
      imports flatMap (_.importers) map expandRelative partition { importer =>
        // Checking `config.expandRelative` is necessary here, because applying `isFullyQualified`
        // on fully-qualified importers expanded from a relative importers always returns false.
        // The reason is that `isFullyQualified` relies on symbol table information, while expanded
        // importers contain synthesized AST nodes without symbols associated with them.
        config.expandRelative || isFullyQualified(importer)
      }

    // Organizes all the fully-qualified global importers.
    val (_, sortedImporterGroups: Seq[Seq[Importer]]) =
      fullyQualifiedImporters
        .flatMap(removeUnused)
        .groupBy(matchImportGroup) // Groups imports by importer prefix.
        .mapValues(organizeImporters) // Organize imports within the same group.
        .toSeq
        .sortBy { case (index, _) => index } // Sorts import groups by group index
        .unzip

    // Append all the relative imports (if any) at the end as a separate group with the original
    // order unchanged.
    val organizedImporterGroups: Seq[Seq[Importer]] =
      if (relativeImporters.isEmpty) sortedImporterGroups
      else sortedImporterGroups :+ relativeImporters.flatMap(removeUnused)

    // A patch that removes all the tokens forming the original imports.
    val removeOriginalImports = Patch.removeTokens(
      doc.tree.tokens.slice(
        imports.head.tokens.start,
        imports.last.tokens.end
      )
    )

    // A patch that inserts the organized imports. Note that global imports within curly-braced
    // packages must be indented accordingly, e.g.:
    //
    //   package foo {
    //     package bar {
    //       import baz
    //       import qux
    //     }
    //   }
    val insertOrganizedImports = {
      val firstImportToken = imports.head.tokens.head
      val indent: Int = firstImportToken.pos.startColumn

      val indentedOutput: Seq[String] =
        organizedImporterGroups
          .map(prettyPrintImportGroup)
          .mkString("\n\n")
          .split("\n")
          .zipWithIndex
          .map {
            // The first line will be inserted at an already indented position.
            case (line, 0)                 => line
            case (line, _) if line.isEmpty => line
            case (line, _)                 => " " * indent + line
          }

      Patch.addLeft(firstImportToken, indentedOutput mkString "\n")
    }

    (removeOriginalImports + insertOrganizedImports).atomic
  }

  private def expandRelative(importer: Importer)(implicit doc: SemanticDocument): Importer = {
    // NOTE: An `Importer.Ref` instance constructed by `toRef` does NOT contain symbol information
    // since it's not parsed from the source file.
    def toRef(symbol: Symbol): Term.Ref =
      if (symbol.owner == Symbol.RootPackage) Term.Name(symbol.displayName)
      else Term.Select(toRef(symbol.owner), Term.Name(symbol.displayName))

    if (!config.expandRelative || isFullyQualified(importer)) importer
    else importer.copy(ref = toRef(importer.ref.symbol.normalized))
  }

  private def organizeImporters(importers: Seq[Importer]): Seq[Importer] = {
    import GroupedImports._
    import ImportsOrder._

    val importeesSorted = {
      config.groupedImports match {
        case Merge   => mergeImporters(importers)
        case Explode => explodeImportees(importers)
        case Keep    => importers
      }
    } map (coalesceImportees _ andThen sortImportees _)

    config.importsOrder match {
      case Ascii =>
        importeesSorted sortBy (_.syntax)

      case SymbolsFirst =>
        // HACK: This is a quick-n-dirty way to achieve the import ordering provided by the IntelliJ
        // IDEA Scala plugin. This implementation does not cover cases like quoted identifiers
        // containg "._" and/or braces.
        importeesSorted sortBy {
          _.syntax
            .replaceAll("\\._$", ".\0")
            .replaceAll("[{}]", "\1")
        }
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
    val (wildcard, withoutWildcard) = importer.importees.partition(_.is[Importee.Wildcard])

    val orderedImportees = config.importSelectorsOrder match {
      case Ascii        => withoutWildcard.sortBy(_.syntax)
      case SymbolsFirst => sortImporteesSymbolsFirst(withoutWildcard)
      case Keep         => withoutWildcard
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
    // NOTE: We need to check whether the input importer is curly braced first and then replace the
    // first "{ " and the last " }" if any. Naive string replacements is not sufficient, e.g., a
    // quoted-identifier like "`{ d }`" may cause broken output.
    val isCurlyBraced = importer.importees match {
      case Importees(_, _ :: _, _, _)        => true // At least one rename
      case Importees(_, _, _ :: _, _)        => true // At least one unimport
      case importees if importees.length > 1 => true // Multiple importees
      case _                                 => false
    }

    val syntax = importer.syntax

    (isCurlyBraced, syntax lastIndexOfSlice " }") match {
      case (true, index) if index > -1 => syntax.patch(index, "}", 2).replaceFirst("\\{ ", "{")
      case _                           => syntax
    }
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
            // Unimports are canceled by the wildcard.
            Seq(renames, names :+ Importee.Wildcard())

          case (false, Some(unimports)) =>
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
        (names ++ renames).map(i => Importer(ref, i :: Nil)) :+ Importer(ref, unimports :+ wildcard)

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
}
