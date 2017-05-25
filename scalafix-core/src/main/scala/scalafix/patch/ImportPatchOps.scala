package scalafix.patch

import scala.collection.immutable.Seq
import scala.collection.mutable
import scalafix.syntax._
import scala.meta._
import scala.meta.tokens.Token.Comment
import scala.meta.tokens.Token.KwImport
import scalafix.Failure
import scalafix.config.FilterMatcher
import scalafix.patch.TreePatch.ImportPatch
import scalafix.rewrite.RewriteCtx
import scalafix.util.Whitespace
import org.scalameta.logger

object ImportPatchOps {

  // NOTE(olafur): This method is the simplest/dummest thing I can think of
  // to convert
  private[scalafix] def superNaiveImportPatchToTokenPatchConverter(
      ctx: RewriteCtx,
      importPatches: Seq[ImportPatch])(
      implicit mirror: Mirror): Iterable[Patch] = {
    val allImports = getGlobalImports(ctx.tree)
    val allImporters = allImports.flatMap(_.importers)
    val allImportees = allImporters.flatMap(_.importees)
    val allImporteeSymbols = allImportees.flatMap { importee =>
      importee.symbolOpt.map(_.normalized -> importee)
    }
    def fallbackToken(tree: Tree): Token = tree match {
      case Source(stat :: _) => fallbackToken(stat)
      case Pkg(_, stat :: _) => fallbackToken(stat)
      case els => ctx.toks(els).head
    }
    val editToken = fallbackToken(ctx.tree)
    val isRemovedImportee = mutable.LinkedHashSet.empty[Importee]
    importPatches.foreach {
      case TreePatch.RemoveGlobalImport(sym) =>
        allImporteeSymbols
          .withFilter(_._1 == sym.normalized)
          .foreach { case (_, x) => isRemovedImportee += x }
      case x: TreePatch.RemoveImportee => isRemovedImportee += x.importee
      case _ =>
    }
    val extraPatches = importPatches.collect {
      case TreePatch.AddGlobalImport(importer)
          if !allImporters.exists(_.syntax == importer.syntax) =>
        ctx.addLeft(editToken, s"import $importer\n")
    }
    val isRemovedImporter =
      allImporters.filter(_.importees.forall(isRemovedImportee)).toSet
    val isRemovedImport =
      allImports.filter(_.importers.forall(isRemovedImporter))
    def remove(toRemove: Tree) = {
      val tokens = ctx.toks(toRemove)
      def removeFirstComma(lst: Iterable[Token]) =
        lst.find(!_.is[Whitespace]) match {
          case Some(tok @ Token.Comma()) => TokenPatch.Remove(tok)
          case _ => Patch.empty
        }
      val trailingComma =
        removeFirstComma(ctx.tokenList.from(tokens.last))
      val leadingComma =
        removeFirstComma(ctx.tokenList.to(tokens.head).reverse)
      trailingComma + leadingComma + PatchOps.removeTokens(tokens)
    }

    extraPatches ++
      (isRemovedImportee ++
        isRemovedImporter ++
        isRemovedImport).map(remove)
  }

  private def extractImports(stats: Seq[Stat]): Seq[Import] = {
    stats
      .takeWhile(_.is[Import])
      .collect { case i: Import => i }
  }

  def getGlobalImports(ast: Tree): Seq[Import] =
    ast match {
      case Pkg(_, Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Source(Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Pkg(_, stats) => extractImports(stats)
      case Source(stats) => extractImports(stats)
      case _ => Nil
    }

}
