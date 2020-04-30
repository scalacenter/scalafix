package fix

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.meta.Import
import scala.meta.Importee
import scala.meta.Importer
import scala.meta.Pkg
import scala.meta.Source
import scala.meta.Stat
import scala.meta.Term
import scala.meta.Tree
import scala.meta.inputs.Position
import scala.util.matching.Regex

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v1._

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

  private def sortImportees(importer: Importer): Importer = {
    import ImportSelectorsOrder._

    config.importSelectorsOrder match {
      case Ascii        => importer.copy(importees = sortImporteesAscii(importer.importees))
      case SymbolsFirst => importer.copy(importees = sortImporteesSymbolsFirst(importer.importees))
      case Keep         => importer
    }
  }

  private def organizeImporters(importers: Seq[Importer]): Seq[Importer] = {
    import GroupedImports._
    import ImportsOrder._

    val importeesSorted = {
      config.groupedImports match {
        case Merge   => mergeImportersWithCommonPrefix(importers)
        case Explode => explodeGroupedImportees(importers)
        case Keep    => importers
      }
    } map sortImportees

    config.importsOrder match {
      case Ascii =>
        importeesSorted sortBy (_.syntax)

      case SymbolsFirst =>
        // Hack: This is a quick-n-dirty way to achieve a the import ordering provided by the
        // IntelliJ IDEA Scala plugin. This implementation does not cover cases like quoted
        // identifiers containg "._" and/or braces.
        importeesSorted sortBy {
          _.syntax
            .replaceAll("\\._$", ".\0")
            .replaceAll("[{}]", "\1")
        }
    }
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

  private def isFullyQualified(importer: Importer)(implicit doc: SemanticDocument): Boolean =
    topQualifierOf(importer.ref).symbol.owner == Symbol.RootPackage

  private def prettyPrintImportGroup(group: Seq[Importer]): String =
    group
      .map(fixedImporterSyntax)
      .map("import " + _)
      .mkString("\n")

  // Hack: The scalafix pretty-printer decides to add spaces after open and before close braces in
  // imports, i.e., "import a.{ b, c }" instead of "import a.{b, c}". Unfortunately, this behavior
  // cannot be overriden. This function removes the unwanted spaces as a workaround.
  private def fixedImporterSyntax(importer: Importer): String =
    importer.syntax
      .replace("{ ", "{")
      .replace(" }", "}")

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => topQualifierOf(qualifier)
      case name: Term.Name           => name
    }

  private def sortImporteesAscii(importees: List[Importee]): List[Importee] = {
    // An `Importer` may contain at most one `Importee.Wildcard`, and it is only allowed to appear
    // at the end of the `Importee` list.
    val (wildcard, withoutWildcard) = importees.partition(_.is[Importee.Wildcard])
    withoutWildcard.sortBy(_.syntax) ++ wildcard
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

  private def mergeImportersWithCommonPrefix(importers: Seq[Importer]): Seq[Importer] =
    importers.groupBy(_.ref.syntax).values.toSeq.map { group =>
      group.head.copy(importees = group.flatMap(_.importees).toList)
    }

  private def explodeGroupedImportees(importers: Seq[Importer]): Seq[Importer] =
    importers.flatMap {
      case Importer(ref, importees) =>
        var containsUnimport = false
        var containsWildcard = false

        val unimportsAndWildcards = importees.collect {
          case i: Importee.Unimport => containsUnimport = true; i :: Nil
          case i: Importee.Wildcard => containsWildcard = true; i :: Nil
          case _                    => Nil
        }.flatten

        if (containsUnimport && containsWildcard) {
          // If an importer contains both `Importee.Unimport`(s) and `Importee.Wildcard`, we must
          // have both of them appearing in a single importer. E.g.:
          //
          //   import scala.collection.{Seq => _, Vector, _}
          //
          // should be rewritten into
          //
          //   import scala.collection.{Seq => _, _}
          //
          // rather than
          //
          //   import scala.collection.Vector
          //   import scala.collection._
          //   import scala.collection.{Seq => _}
          //
          // Especially, we don't need `Vector` in the result since it's already covered by the
          // wildcard import.
          Importer(ref, unimportsAndWildcards) :: Nil
        } else {
          importees.map(importee => Importer(ref, importee :: Nil))
        }
    }
}
