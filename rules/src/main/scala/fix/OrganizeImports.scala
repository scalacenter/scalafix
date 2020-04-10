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
  def matches(i: Importer): Boolean
}

case class RegexMatcher(pattern: Regex) extends ImportMatcher {
  override def matches(i: Importer): Boolean = (pattern findPrefixMatchOf i.syntax).nonEmpty
}

case class PlainTextMatcher(pattern: String) extends ImportMatcher {
  override def matches(i: Importer): Boolean = i.syntax startsWith pattern
}

case object WildcardMatcher extends ImportMatcher {
  // We don't want the "*" wildcard group to match anything since it is always special-cased at the
  // end of the import group matching process.
  def matches(importer: Importer): Boolean = false
}

class OrganizeImports(config: OrganizeImportsConfig) extends SemanticRule("OrganizeImports") {
  private val importMatchers = config.groups map {
    case p if p startsWith "re:" => RegexMatcher(new Regex(p stripPrefix "re:"))
    case "*"                     => WildcardMatcher
    case p                       => PlainTextMatcher(p)
  }

  private val wildcardGroupIndex = importMatchers indexOf WildcardMatcher

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
    val (fullyQualifiedImporters, relativeImporters) =
      imports flatMap (_.importers) partition { importer =>
        topQualifierOf(importer.ref).symbol.owner == Symbol.RootPackage
      }

    val (_, sortedFullyQualifiedGroups: Seq[Seq[Importer]]) =
      fullyQualifiedImporters
        .groupBy(matchImportGroup(_, importMatchers)) // Groups imports by importer prefix.
        .mapValues(_ sortBy fixedImporterSyntax) // Sorts all the imports within the same group.
        .toSeq
        .sortBy { case (index, _) => index } // Sorts import groups by group index
        .unzip

    // Append all the relative imports at the end as a separate group with the original order
    // unchanged.
    val resultImporterGroups: Seq[Seq[Importer]] =
      if (relativeImporters.isEmpty) sortedFullyQualifiedGroups
      else sortedFullyQualifiedGroups :+ relativeImporters

    // A patch that inserts all the organized imports.
    val insertOrganizedImports = Patch.addLeft(
      imports.head,
      resultImporterGroups
        .map(_ map fixedImporterSyntax map ("import " + _) mkString "\n")
        .mkString("\n\n")
    )

    // A patch that removes all the original imports.
    val removeOriginalImports = Patch.removeTokens(
      doc.tree.tokens.slice(
        imports.head.tokens.start,
        imports.last.tokens.end
      )
    )

    (insertOrganizedImports + removeOriginalImports).atomic
  }

  @tailrec private def topQualifierOf(term: Term): Term.Name =
    term match {
      case Term.Select(qualifier, _) => topQualifierOf(qualifier)
      case name: Term.Name           => name
    }

  // The scalafix pretty-printer decides to add spaces after open and before close braces in
  // imports, i.e., "import a.{ b, c }" instead of "import a.{b, c}". Unfortunately, this behavior
  // cannot be overriden. This function removes the unwanted spaces as a workaround.
  private def fixedImporterSyntax(importer: Importer)(implicit doc: SemanticDocument): String =
    importer.syntax.replace("{ ", "{").replace(" }", "}")

  // Returns the index of the group a given importer belongs to.
  private def matchImportGroup(importer: Importer, matchers: Seq[ImportMatcher]): Int = {
    val index = matchers indexWhere (_ matches importer)
    if (index > -1) index else wildcardGroupIndex
  }

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
