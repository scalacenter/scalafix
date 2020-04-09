package fix

import scala.annotation.tailrec
import scala.meta.{Import, Importer, Pkg, Source, Term, Tree}
import scala.util.matching.Regex

import metaconfig.Configured
import metaconfig.generic.{Surface, deriveDecoder, deriveEncoder, deriveSurface}
import metaconfig.{ConfDecoder, ConfEncoder}
import scalafix.patch.Patch
import scalafix.v1._

final case class OrganizeImportsConfig(
  groups: Seq[String] = Seq(
    "re:javax?\\.",
    "scala.",
    "*"
  )
)

object OrganizeImportsConfig {
  val default: OrganizeImportsConfig = OrganizeImportsConfig()

  implicit val surface: Surface[OrganizeImportsConfig] =
    deriveSurface[OrganizeImportsConfig]

  implicit val decoder: ConfDecoder[OrganizeImportsConfig] =
    deriveDecoder[OrganizeImportsConfig](default)

  implicit val encoder: ConfEncoder[OrganizeImportsConfig] =
    deriveEncoder[OrganizeImportsConfig]
}

sealed trait ImportMatcher {
  def matchPrefix(importer: Importer): Boolean
}

case class RegexMatcher(pattern: String) extends ImportMatcher {
  private val regex: Regex = new Regex(pattern)

  override def matchPrefix(importer: Importer): Boolean =
    (regex findPrefixMatchOf importer.syntax).nonEmpty
}

case class PlainTextMatcher(pattern: String) extends ImportMatcher {
  override def matchPrefix(importer: Importer): Boolean = importer.syntax startsWith pattern
}

case object WildcardMatcher extends ImportMatcher {
  // We don't want the "*" wildcard group to match anything since it is always special-cased at the
  // end of the import group matching process.
  def matchPrefix(importer: Importer): Boolean = false
}

class OrganizeImports(config: OrganizeImportsConfig) extends SemanticRule("OrganizeImports") {
  def this() = this(OrganizeImportsConfig())

  override def isExperimental: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("OrganizeImports")(OrganizeImportsConfig()).map { c =>
      // The "*" wildcard group should always exist. Append one at the end if omitted.
      val withStar = if (c.groups contains "*") c.groups else c.groups :+ "*"
      new OrganizeImports(c.copy(groups = withStar))
    }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val globalImports = collectGlobalImports(doc.tree)
    if (globalImports.isEmpty) Patch.empty else organizeImports(globalImports)
  }

  private def organizeImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val (fullyQualifiedImporters, relativeImporters) = imports flatMap (_.importers) partition {
      importer => firstQualifier(importer.ref).symbol.owner == Symbol.RootPackage
    }

    val (_, organizedImportGroups: Seq[String]) = {
      val importMatchers = config.groups map {
        case p if p startsWith "re:" => RegexMatcher(p stripPrefix "re:")
        case "*"                     => WildcardMatcher
        case p                       => PlainTextMatcher(p)
      }

      fullyQualifiedImporters
        .groupBy(matchImportGroup(_, importMatchers))
        .mapValues(organizeImportGroup)
        .toSeq
        .sortBy { case (index, _) => index }
        .unzip
    }

    // A patch that removes all the original imports.
    val removeOriginalImports = Patch.removeTokens(
      doc.tree.tokens.slice(
        imports.head.tokens.start,
        imports.last.tokens.end
      )
    )

    // A patch that inserts all the organized imports, with all the relative imports appended at the
    // end as a separate group.
    val insertOrganizedImports = {
      val relativeImportGroup =
        if (relativeImporters.isEmpty) Nil
        else relativeImporters.map("import " + _.syntax).mkString("\n") :: Nil

      Patch.addLeft(
        imports.head,
        (organizedImportGroups ++ relativeImportGroup) mkString "\n\n"
      )
    }

    insertOrganizedImports + removeOriginalImports
  }

  @tailrec private def firstQualifier(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => firstQualifier(qualifier)
      case t: Term.Name              => t
    }

  // Returns the index of the group a given importer belongs to.
  private def matchImportGroup(importer: Importer, matchers: Seq[ImportMatcher]): Int = {
    val index = matchers indexWhere (_ matchPrefix importer)
    if (index > -1) index else matchers indexOf WildcardMatcher
  }

  private def organizeImportGroup(importers: Seq[Importer]): String =
    importers.map("import " + _.syntax).sorted.mkString("\n")

  private object / {
    def unapply(tree: Tree): Option[(Tree, Tree)] = tree.parent map (_ -> tree)
  }

  private def collectGlobalImports(tree: Tree): Seq[Import] = tree match {
    case s: Source                 => s.children flatMap collectGlobalImports
    case (_: Source) / (p: Pkg)    => p.children flatMap collectGlobalImports
    case (_: Pkg) / (p: Pkg)       => p.children flatMap collectGlobalImports
    case (_: Source) / (i: Import) => Seq(i)
    case (_: Pkg) / (i: Import)    => Seq(i)
    case _                         => Seq.empty[Import]
  }
}
