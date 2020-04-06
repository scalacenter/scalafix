package fix

import scala.annotation.tailrec
import scala.meta.{Import, Importer, Pkg, Source, Term, Tree}

import metaconfig.Configured
import metaconfig.generic.{deriveDecoder, deriveEncoder, deriveSurface, Surface}
import metaconfig.{ConfDecoder, ConfEncoder}
import scalafix.patch.Patch
import scalafix.v1._

final case class OrganizeImportsConfig(
  sortImportees: Boolean = false,
  groups: Seq[String] = Seq("*")
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

class OrganizeImports(config: OrganizeImportsConfig) extends SemanticRule("OrganizeImports") {
  def this() = this(OrganizeImportsConfig())

  override def isExperimental: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("OrganizeImports")(OrganizeImportsConfig()).map { c =>
      // The "*" group should always exist. If the user didn't provide one, append one at the end.
      val withStar = if (c.groups contains "*") c.groups else c.groups :+ "*"
      new OrganizeImports(c.copy(groups = withStar))
    }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val globalImports = collectGlobalImports(doc.tree)
    if (globalImports.isEmpty) Patch.empty else organizeImports(globalImports)
  }

  private def organizeImports(imports: Seq[Import])(implicit doc: SemanticDocument): Patch = {
    val (fullyQualifiedImporters, relativeImporters) = imports flatMap (_.importers) partition {
      importerRefFirstName(_).symbol.owner == Symbol.RootPackage
    }

    val (_, organizedImportGroups: Seq[String]) =
      fullyQualifiedImporters
        .groupBy(getImportGroup(_, config.groups))
        .mapValues(organizeImportGroup)
        .mapValues(_ map (_.syntax) mkString "\n")
        .toSeq
        .sortBy { case (index, _) => index }
        .unzip

    val relativeImportGroup = relativeImporters map ("import " + _.syntax) mkString "\n"

    val insertOrganizedImports = Patch.addLeft(
      imports.head,
      (organizedImportGroups :+ relativeImportGroup) mkString "\n\n"
    )

    val removeOriginalImports = Patch.removeTokens(
      doc.tree.tokens.slice(
        imports.head.tokens.start,
        imports.last.tokens.end
      )
    )

    insertOrganizedImports + removeOriginalImports
  }

  def importerRefFirstName(importer: Importer): Term.Name = {
    @tailrec def loop(term: Term): Term.Name = term match {
      case Term.Select(qualifier, _) => loop(qualifier)
      case t: Term.Name              => t
    }

    loop(importer.ref)
  }

  // Returns the index of the group a given importer belongs to.
  private def getImportGroup(importer: Importer, groups: Seq[String]): Int = {
    val index = groups filterNot (_ == "*") indexWhere (importer.syntax startsWith _)
    if (index > -1) index else groups indexOf "*"
  }

  private def organizeImportGroup(importers: Seq[Importer]): Seq[Import] =
    importers sortBy (_.syntax) map sortImportees map (i => Import(i :: Nil))

  private def sortImportees(importer: Importer): Importer =
    if (!config.sortImportees) importer
    else importer.copy(importees = importer.importees sortBy (_.syntax))

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
