package scalafix.internal.patch

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta._
import scala.meta.tokens.Token.Comment
import scala.meta.tokens.Token.KwImport
import scalafix.config.FilterMatcher
import scalafix.patch.Patch
import scalafix.patch.TokenPatch
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch.ImportPatch
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.Newline

object ImportPatchOps {

  // NOTE(olafur): This method is the simplest/dummest thing I can think of
  // to convert
  private[scalafix] def superNaiveImportPatchToTokenPatchConverter(
      ctx: RewriteCtx,
      importPatches: Seq[ImportPatch])(
      implicit mirror: Mirror): Iterable[Patch] = {
    val allImports = ctx.tree.collect { case i: Import => i }
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
      allImporters.toIterator
        .filter(_.importees.forall(isRemovedImportee))
        .toSet
    val curlyBraceRemoves = allImporters.map { importer =>
      val keptImportees = importer.importees.filterNot(isRemovedImportee)
      val hasRemovedImportee = importer.importees.exists(isRemovedImportee)
      keptImportees match {
        case (Importee.Wildcard() | Importee.Name(_)) +: Nil
            if hasRemovedImportee =>
          ctx
            .toks(importer)
            .collectFirst {
              case open @ Token.LeftBrace() =>
                ctx.matching
                  .close(open)
                  .map(close =>
                    ctx.removeToken(open) +
                      ctx.removeToken(close))
                  .asPatch
            }
            .asPatch
        case _ => Patch.empty
      }
    }
    // NOTE: keeps track of which comma is removed by which tree to prevent the
    // same comma being removed twice.
    val isRemovedComma = mutable.Map.empty[Token.Comma, Tree]
    val isRemovedImport =
      allImports.filter(_.importers.forall(isRemovedImporter))
    def remove(toRemove: Tree) = {
      val tokens = ctx.toks(toRemove)
      def removeFirstComma(lst: Iterable[Token]) = {
        lst
          .takeWhile {
            case Token.Space() => true
            case comma @ Token.Comma() =>
              if (!isRemovedComma.contains(comma)) {
                isRemovedComma(comma) = toRemove
              }
              true
            case _ => false
          }
          .map(ctx.removeToken(_))
      }
      val leadingComma =
        removeFirstComma(ctx.tokenList.leading(tokens.head))
      val hadLeadingComma = leadingComma.exists {
        case TokenPatch.Add(comma: Token.Comma, _, _, keepTok @ false) =>
          isRemovedComma.get(comma).contains(toRemove)
        case _ => false
      }
      val trailingComma =
        if (hadLeadingComma) List(Patch.empty)
        else removeFirstComma(ctx.tokenList.trailing(tokens.last))
      ctx.removeTokens(tokens) ++ trailingComma ++ leadingComma
    }

    val leadingNewlines = isRemovedImport.map { i =>
      var newline = false
      ctx.tokenList
        .leading(ctx.toks(i).head)
        .takeWhile(x =>
          !newline && {
            x.is[Token.Space] || {
              val isNewline = x.is[Newline]
              if (isNewline) newline = true
              isNewline
            }
        })
        .map(tok => ctx.removeToken(tok))
        .asPatch
    }

    leadingNewlines ++
      curlyBraceRemoves ++
      extraPatches ++
      (isRemovedImportee ++
        isRemovedImporter ++
        isRemovedImport).map(remove)
  }

}
