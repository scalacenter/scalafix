package scalafix.util

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.Importee.Wildcard
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.tokens.Token.Comment
import scala.meta.tokens.Token.KwImport
import scalafix.config.FilterMatcher
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.RemoveGlobalImport

/** Set of operations needed to run organize imports */
trait OrganizeImportsMirror {

  /** Returns true if this importee is never used and can be removed. */
  def isUnused(importee: Importee): Boolean

  /** Returns fully qualified name of this reference
    *
    * For example scala.collection.immutable.List for List.
    **/
  def fullyQualifiedName(ref: Ref): Option[Ref]
}

private[this] class OrganizeImports[T] private (implicit ctx: RewriteCtx[T],
                                                ev: CanOrganizeImports[T]) {
  val mirror: OrganizeImportsMirror = ev.toOrganizeImportsMirror(ctx.mirror)
  def extractImports(stats: Seq[Stat]): Seq[Import] = {
    stats
      .takeWhile(_.is[Import])
      .collect { case i: Import => i }
  }

  def getCanonicalImports(imp: Import): Seq[CanonicalImport] = {
    implicit val currentImport = imp
    imp.importers.flatMap { importer =>
      val wildcard = importer.importees.lastOption.collectFirst {
        case wildcard: Importee.Wildcard => wildcard
      }
      wildcard.fold(
        importer.importees.map(i =>
          CanonicalImport.fromImportee(importer.ref, i))
      ) { wildcard =>
        List(
          CanonicalImport.fromWildcard(
            importer.ref,
            wildcard,
            importer.importees.init
          ))
      }
    }
  }

  def getLastTopLevelPkg(potPkg: Stat): Stat = potPkg match {
    case Pkg(_, head +: Nil) => getLastTopLevelPkg(head)
    case Pkg(_, head +: _) => head
    case _ => potPkg
  }

  def getGlobalImports(ast: Tree): Seq[Import] =
    ast match {
      case Pkg(_, Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Source(Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Pkg(_, stats) => extractImports(stats)
      case Source(stats) => extractImports(stats)
      case _ => Nil
    }

  def removeDuplicates(imports: Seq[CanonicalImport]): Seq[CanonicalImport] =
    imports.distinctBy(_.importerSyntax)

  def removeUnused(
      possiblyDuplicates: Seq[CanonicalImport]): Seq[CanonicalImport] = {
    val imports = removeDuplicates(possiblyDuplicates)
    if (!ctx.config.imports.removeUnused) imports
    else {
      imports.collect {
        case i if !mirror.isUnused(i.importee) =>
          i.copy(extraImportees = i.extraImportees.filterNot(mirror.isUnused))
      }
    }
  }

  def rootPkgName(ref: Ref): String = ref match {
    case name: Term.Name => name.value
    case _ =>
      ref.collect {
        case Term.Select(name: Term.Name, _) => name.value
      }.head
  }

  def fullyQualify(imp: CanonicalImport): Option[Term.Ref] =
    for {
      fqnRef <- mirror.fullyQualifiedName(imp.ref)
      // Avoid inserting unneeded `package`
      if rootPkgName(fqnRef) != rootPkgName(imp.ref)
    } yield fqnRef.asInstanceOf[Term.Ref]

  def groupImports(imports0: Seq[CanonicalImport]): Seq[Seq[Import]] = {
    val config = ctx.config.imports
    val rootImports = imports0.filter(_.isRootImport)
    val imports =
      imports0.map(imp => imp.withFullyQualifiedRef(fullyQualify(imp), rootImports))
    val (fullyQualifiedImports, relativeImports) =
      imports.partition { imp =>
        ctx.config.imports.expandRelative ||
        fullyQualify(imp).forall(_.syntax == imp.ref.syntax)
      }
    val groupById =
      config.groups.zipWithIndex.toMap
        .withDefaultValue(config.groups.length)
    val grouped: Map[FilterMatcher, Seq[CanonicalImport]] =
      fullyQualifiedImports
        .groupBy { imp =>
          config.groups
            .find(_.matches(imp.refSyntax))
            .getOrElse(FilterMatcher.matchEverything)
        } + (FilterMatcher("relative") -> relativeImports)
    val inOrder =
      grouped
        .mapValues(x => x.sortBy(_.sortOrder))
        .to[Seq]
        .filter(_._2.nonEmpty)
        .sortBy(x => groupById(x._1))
        .collect { case (_, s) => s }
    val asImports = inOrder.map { is =>
      if (config.groupByPrefix) {
        is.groupBy(_.ref.syntax)
          .to[Seq]
          .map {
            case (_, importers) =>
              Import(Seq(
                Importer(importers.head.actualRef, importers.map(_.importee))))

          }
      } else {
        var usedLeadingComment = Set.empty[Comment]
        is.map { i =>
          val result = i
            .withoutLeading(usedLeadingComment)
            .syntax
            .parse[Stat]
            .get
            .asInstanceOf[Import]
          usedLeadingComment = usedLeadingComment ++ i.leadingComments
          result
        }
      }
    }
    asImports
  }

  def prettyPrint(imports: Seq[CanonicalImport]): String = {
    groupImports(imports)
      .map(_.map(_.syntax).mkString("\n"))
      .mkString("\n\n")
  }

  def getRemovePatches(oldImports: Seq[Import]): Seq[TokenPatch.Remove] = {
    val toRemove = for {
      firstImport <- oldImports.headOption
      first <- firstImport.tokens.headOption
      lastImport <- oldImports.lastOption
      last <- lastImport.tokens.lastOption
    } yield {
      ctx.tokens.toIterator
        .dropWhile(_.start < first.start)
        .takeWhile { x =>
          x.end <= last.end
        }
        .map(TokenPatch.Remove)
        .toList
    }
    toRemove.getOrElse(Nil)
  }

  def cleanUpImports(globalImports: Seq[CanonicalImport],
                     patches: Seq[ImportPatch]): Seq[CanonicalImport] = {
    def combine(is: Seq[CanonicalImport],
                patch: ImportPatch): Seq[CanonicalImport] =
      patch match {
        case _: AddGlobalImport =>
          is ++ getCanonicalImports(patch.toImport)
        case remove: RemoveGlobalImport =>
          is.filterNot(_.structure == remove.importer.structure)
      }
    patches.foldLeft(removeUnused(globalImports))(combine)
  }

  lazy val fallbackToken: Token = {
    def loop(tree: Tree): Token = tree match {
      case Source(stat :: _) => loop(stat)
      case Pkg(_, stat :: _) => loop(stat)
      case els => els.tokens(ctx.config.dialect).head
    }
    loop(ctx.tree)
  }

  def organizeImports(patches: Seq[ImportPatch]): Seq[TokenPatch] = {
    if (!ctx.config.imports.organize && patches.isEmpty) {
      Nil
    } else {
      val oldImports = getGlobalImports(ctx.tree)
      val globalImports = oldImports.flatMap(getCanonicalImports)
      val cleanedUpImports = cleanUpImports(globalImports, patches)
      val tokenToEdit =
        oldImports.headOption
          .map(_.tokens.head)
          .getOrElse(fallbackToken)
      val suffix =
        if (!tokenToEdit.is[KwImport] && tokenToEdit.eq(fallbackToken)) "\n"
        else ""
      val toInsert = prettyPrint(cleanedUpImports) ++ suffix
      TokenPatch.AddLeft(tokenToEdit, toInsert) +:
        getRemovePatches(oldImports)
    }
  }
}

object OrganizeImports {
  def organizeImports[T: CanOrganizeImports](patches: Seq[ImportPatch])(
      implicit ctx: RewriteCtx[T]): Seq[TokenPatch] =
    new OrganizeImports().organizeImports(
      patches ++ ctx.config.patches.all.collect {
        case i: ImportPatch => i
      }
    )
}
