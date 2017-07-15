package scalafix.internal.patch

import scala.annotation.tailrec
import scala.collection.immutable.Seq
import scala.collection.mutable
import scalafix.internal.util.SymbolOps
import scalafix.patch.Patch
import scalafix.patch.TokenPatch
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch.ImportPatch
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.Newline
import scala.meta._

object ImportPatchOps {

  private def fallbackToken(ctx: RewriteCtx): Token = {
    def loop(tree: Tree): Token = tree match {
      case Source((stat: Pkg) :: _) => loop(stat)
      case Source(_) => ctx.toks(tree).head
      case Pkg(_, stat :: _) => loop(stat)
      case els => ctx.tokenList.prev(ctx.tokenList.prev(ctx.toks(els).head))
    }
    loop(ctx.tree)
  }
  private def extractImports(stats: Seq[Stat]): Seq[Import] = {
    stats
      .takeWhile(_.is[Import])
      .collect { case i: Import => i }
  }

  @tailrec private final def getLastTopLevelPkg(potPkg: Stat): Stat =
    potPkg match {
      case Pkg(_, head +: Nil) => getLastTopLevelPkg(head)
      case Pkg(_, head +: _) => head
      case _ => potPkg
    }

  @tailrec private final def getGlobalImports(ast: Tree): Seq[Import] =
    ast match {
      case Pkg(_, Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Source(Seq(pkg: Pkg)) => getGlobalImports(pkg)
      case Pkg(_, stats) => extractImports(stats)
      case Source(stats) => extractImports(stats)
      case _ => Nil
    }

  // NOTE(olafur): This method is the simplest/dummest thing I can think of
  // to convert
  private[scalafix] def superNaiveImportPatchToTokenPatchConverter(
      ctx: RewriteCtx,
      importPatches: Seq[ImportPatch])(
      implicit mirror: Mirror): Iterable[Patch] = {
    val allImports = ctx.tree.collect { case i: Import => i }
    val allImporters = allImports.flatMap(_.importers)
    val allImportees = allImporters.flatMap(_.importees)
    val allNamedImports = allImportees.collect {
      case Importee.Name(n) if mirror.database.names.contains(n.pos) =>
        n.symbol
      // TODO(olafur) handle rename.
    }
    val allImporteeSymbols = allImportees.flatMap(importee =>
      importee.symbolOpt.map(_.normalized -> importee))
    val editToken: Token = {
      val imports = getGlobalImports(ctx.tree)
      if (imports.isEmpty) fallbackToken(ctx)
      else ctx.toks(imports.last).last
    }
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
      case TreePatch.AddGlobalSymbol(symbol)
          if !allNamedImports.contains(symbol) =>
        SymbolOps
          .toImporter(symbol)
          .fold(Patch.empty)(importer =>
            ctx.addRight(editToken, s"\nimport $importer"))
      case TreePatch.AddGlobalImport(importer)
          if !allImporters.exists(_.syntax == importer.syntax) =>
        ctx.addRight(editToken, s"\nimport $importer")
    }
    val isRemovedImporter =
      allImporters.toIterator
        .filter(_.importees.forall(isRemovedImportee))
        .toSet
    def removeSpaces(tokens: scala.Seq[Token]): Patch =
      tokens
        .takeWhile {
          case Token.Space() => true
          case _ => false
        }
        .map(ctx.removeToken(_))
        .asPatch
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
                  .map { close =>
                    ctx.removeToken(open) +
                      removeSpaces(ctx.tokenList.trailing(open)) +
                      ctx.removeToken(close) +
                      removeSpaces(ctx.tokenList.leading(close))
                  }
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
