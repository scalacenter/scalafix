package scalafix.util

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.Importee.Wildcard
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.tokens.Token.Comment
import scalafix.FilterMatcher
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.RemoveGlobalImport

private[this] class OrganizeImports private (implicit ctx: RewriteCtx) {
  def extractImports(stats: Seq[Stat]): Seq[Import] = {
    stats
      .takeWhile(_.is[Import])
      .collect { case i: Import => i }
  }

  def getCanonicalImports(imp: Import): Seq[CanonicalImport] = {
    implicit val currentImport = imp
    imp.importers.flatMap { importer =>
      val wildcard = importer.importees.collectFirst {
        case wildcard: Importee.Wildcard => wildcard
      }
      wildcard.fold(
        importer.importees.map(i =>
          CanonicalImport.fromImportee(importer.ref, i))
      ) { wildcard =>
        val unimports = importer.importees.collect {
          case i: Importee.Unimport => i
        }
        val renames = importer.importees.collect {
          case i: Importee.Rename => i
        }
        List(
          CanonicalImport.fromWildcard(
            importer.ref,
            wildcard,
            unimports,
            renames
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

  def removeDuplicates(imports: Seq[CanonicalImport]): Seq[CanonicalImport] = {
    val usedSyntax = mutable.Set.empty[String]
    val wildcards = imports.collect {
      case c if c.importee.is[Importee.Wildcard] => c.ref.syntax
    }.toSet
    def isDuplicate(imp: CanonicalImport): Boolean = {
      val plainSyntax = imp.tree.syntax
      if (usedSyntax.contains(plainSyntax)) true
      else {
        usedSyntax += plainSyntax
        imp.importee match {
          case _: Importee.Name => wildcards.contains(imp.ref.syntax)
          case _ => false
        }
      }
    }
    imports.filterNot(isDuplicate)
  }

  def removeUnused(
      possiblyDuplicates: Seq[CanonicalImport]): Seq[CanonicalImport] = {
    val imports = removeDuplicates(possiblyDuplicates)
    if (!ctx.config.imports.removeUnused) imports
    else {
      val (usedImports, unusedImports) =
        ctx.semantic
          .map { semantic =>
            imports.partition(i => !semantic.isUnusedImport(i.importee))
          }
          .getOrElse(possiblyDuplicates -> Nil)
      usedImports
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
      semantic <- ctx.semantic
//      sym <- semantic.symbol(imp.ref).toOption
//      fqnRef <- sym.toTermRef
      fqnRef <- semantic.fqn(imp.ref)
      // Avoid inserting unneeded `package`
      if rootPkgName(fqnRef) != rootPkgName(imp.ref)
    } yield fqnRef.asInstanceOf[Term.Ref]

  def groupImports(imports0: Seq[CanonicalImport]): Seq[Seq[Import]] = {
    val config = ctx.config.imports
    val imports =
      imports0.map(imp => imp.withFullyQualifiedRef(fullyQualify(imp)))
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
            .getOrElse(config.groups.last)
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
          if (is.exists(_.supersedes(patch))) is
          else is ++ getCanonicalImports(patch.toImport)
        case remove: RemoveGlobalImport =>
          is.filterNot(_.structure == remove.importer.structure)
      }
    patches.foldLeft(removeUnused(globalImports))(combine)
  }

  def organizeImports(code: Tree, patches: Seq[ImportPatch]): Seq[TokenPatch] = {
    if (!ctx.config.imports.organize && patches.isEmpty) {
      Nil
    } else {
      val oldImports = getGlobalImports(code)
      val globalImports = oldImports.flatMap(getCanonicalImports)
      val cleanedUpImports = cleanUpImports(globalImports, patches)
      val tokenToEdit =
        oldImports.headOption
          .map(_.tokens.head)
          .getOrElse(ctx.tokens.head)
      val toInsert = prettyPrint(cleanedUpImports)
      TokenPatch.AddLeft(tokenToEdit, toInsert) +:
        getRemovePatches(oldImports)
    }
  }
}

object OrganizeImports {
  def organizeImports(code: Tree, patches: Seq[ImportPatch])(
      implicit ctx: RewriteCtx): Seq[TokenPatch] =
    new OrganizeImports().organizeImports(code, patches)
}
