package scalafix.util

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.Importee.Wildcard
import scala.meta._
import scala.meta.tokens.Token.Comment
import scalafix.FilterMatcher
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.RemoveGlobalImport

object OrganizeImports {
  def extractImports(stats: Seq[Stat])(
      implicit ctx: RewriteCtx): (Seq[Import], Seq[CanonicalImport]) = {
    val imports = stats.takeWhile(_.is[Import]).collect { case i: Import => i }
    val importees = imports.collect {
      case imp @ Import(importers) =>
        implicit val currentImport = imp
        importers.flatMap { importer =>
          val ref = importer.ref //  fqnRef.getOrElse(importer.ref)
          val wildcard = importer.importees.collectFirst {
            case wildcard: Importee.Wildcard => wildcard
          }
          wildcard.fold(importer.importees.map(i => CanonicalImport(ref, i))) {
            wildcard =>
              val unimports = importer.importees.collect {
                case i: Importee.Unimport => i
              }
              val renames = importer.importees.collect {
                case i: Importee.Rename => i
              }
              List(CanonicalImport(ref, wildcard, unimports, renames))
          }
        }
    }.flatten
    imports -> importees
  }

  def getLastTopLevelPkg(potPkg: Stat): Stat = potPkg match {
    case Pkg(_, head +: Nil) => getLastTopLevelPkg(head)
    case Pkg(_, head +: _) => head
    case _ => potPkg
  }

  def getGlobalImports(ast: Tree)(
      implicit ctx: RewriteCtx): (Seq[Import], Seq[CanonicalImport]) =
    ast match {
      case Pkg(_, Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Source(Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Pkg(_, stats) => extractImports(stats)
      case Source(stats) => extractImports(stats)
      case _ => Nil -> Nil
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

  def removeUnused(possiblyDuplicates: Seq[CanonicalImport])(
      implicit ctx: RewriteCtx): Seq[CanonicalImport] = {
    val imports = removeDuplicates(possiblyDuplicates)
    if (!ctx.config.imports.removeUnused) imports
    else {
      val (usedImports, unusedImports) =
        ctx.semantic
          .map { semantic =>
            val unusedImports =
              imports.partition(i => !semantic.isUnusedImport(i.importee))
            unusedImports
          }
          .getOrElse(possiblyDuplicates -> Nil)
      usedImports
    }
  }

  def groupImports(imports0: Seq[CanonicalImport])(
      implicit ctx: RewriteCtx): Seq[Seq[Import]] = {
    val config = ctx.config.imports
    def fullyQualify(imp: CanonicalImport): Option[Term.Ref] =
      for {
        semantic <- ctx.semantic
        fqnRef <- semantic.fqn(imp.ref)
        if fqnRef.is[Term.Ref]
      } yield fqnRef.asInstanceOf[Term.Ref]
    val imports =
      imports0.map(imp => imp.withFullyQualifiedRef(fullyQualify(imp)))
    val (fullyQualifiedImports, relativeImports) =
      imports.partition { imp =>
        ctx.config.imports.expandRelative ||
        fullyQualify(imp).exists(_.syntax == imp.ref.syntax)
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

  def prettyPrint(imports: Seq[CanonicalImport])(
      implicit ctx: RewriteCtx): String = {
    groupImports(imports)
      .map(_.map(_.syntax).mkString("\n"))
      .mkString("\n\n")
  }

  def organizeImports(code: Tree, patches: Seq[ImportPatch])(
      implicit ctx: RewriteCtx): Seq[TokenPatch] = {
    if (!ctx.config.imports.organize && patches.isEmpty) {
      Nil
    } else {
      def combine(is: Seq[CanonicalImport],
                  patch: ImportPatch): Seq[CanonicalImport] =
        patch match {
          case add: AddGlobalImport =>
            if (is.exists(_.supersedes(patch))) is
            else is :+ patch.importer
          case remove: RemoveGlobalImport =>
            is.filter(_.structure == remove.importer.structure)
        }
      val (oldImports, globalImports) = getGlobalImports(code)
      val allImports =
        patches.foldLeft(removeUnused(globalImports))(combine)
      groupImports(allImports)
      val tokens = code.tokens
      val tok =
        oldImports.headOption.map(_.tokens.head).getOrElse(tokens.head)
      val toRemove = for {
        firstImport <- oldImports.headOption
        first <- firstImport.tokens.headOption
        lastImport <- oldImports.lastOption
        last <- lastImport.tokens.lastOption
      } yield {
        tokens.toIterator
          .dropWhile(_.start < first.start)
          .takeWhile { x =>
            x.end <= last.end
          }
          .map(TokenPatch.Remove)
          .toList
      }
      val toInsert = prettyPrint(allImports)
      TokenPatch.AddLeft(tok, toInsert) +:
        toRemove.getOrElse(Nil)
    }
  }
}
