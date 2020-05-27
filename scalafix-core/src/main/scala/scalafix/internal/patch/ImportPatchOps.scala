package scalafix.internal.patch

import scala.annotation.tailrec
import scala.collection.mutable
import scala.meta._
import scalafix.internal.util.SymbolOps
import scalafix.patch.Patch
import scalafix.patch.Patch.internal._
import scalafix.rule.RuleCtx
import scalafix.syntax._
import scalafix.util.Newline
import scalafix.util.SemanticdbIndex
import scalafix.v0.Signature
import scalafix.v0.Symbol

object ImportPatchOps {
  object symbols {
    val Scala: Symbol = Symbol("_root_/scala/")
    val Predef: Symbol = Symbol("_root_/scala/Predef.")
    val Java: Symbol = Symbol("_root_/java/lang/")
    val Immutable: Symbol = Symbol("_root_/scala/collection/immutable/")
  }

  def isPredef(symbol: Symbol): Boolean = {
    import symbols._
    symbol match {
      case Symbol.Global(`Immutable`, Signature.Type("List" | "Map" | "Set")) =>
        true
      case Symbol.Global(owner, _) =>
        owner == Scala ||
          owner == Predef ||
          owner == Java
      case _ => false
    }
  }
  private def fallbackToken(ctx: RuleCtx): Token = {
    def loop(tree: Tree): Token = tree match {
      case Source((stat: Pkg) :: _) => loop(stat)
      case Source(_) => ctx.toks(tree).head
      case Pkg(_, stat :: _) => loop(stat)
      case els =>
        ctx.tokenList.prev(ctx.tokenList.prev(ctx.toks(els).head)) match {
          case comment @ Token.Comment(_) =>
            ctx.tokenList.prev(ctx.tokenList.prev(comment))
          case other => other
        }
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
  private[scalafix] def superNaiveImportPatchToTokenPatchConverter(
      ctx: RuleCtx,
      importPatches: Seq[ImportPatch]
  )(implicit index: SemanticdbIndex): Iterable[Patch] = {
    val allImports = ctx.tree.collect { case i: Import => i }
    val allImporters = allImports.flatMap(_.importers)
    lazy val allGlobalImportersSyntax = (for {
      importer <- allImporters.iterator
      p1 <- importer.parent
      p2 <- p1.parent
      if p2.is[Pkg]
    } yield importer.syntax).toSet
    val allImportees = allImporters.flatMap(_.importees)
    lazy val allNamedImports = allImportees.collect {
      case Importee.Name(n) if index.symbol(n).isDefined =>
        n.symbol
      // TODO(olafur) handle rename.
    }
    lazy val allImporteeSymbols = allImportees.flatMap(importee =>
      importee.symbol.map(_.normalized -> importee)
    )
    val globalImports = getGlobalImports(ctx.tree)
    val editToken: Token = {
      if (globalImports.isEmpty) fallbackToken(ctx)
      else ctx.toks(globalImports.last).last
    }
    val isRemovedImportee = mutable.LinkedHashSet.empty[Importee]
    importPatches.foreach {
      case RemoveGlobalImport(sym) =>
        allImporteeSymbols
          .withFilter(_._1 == sym.normalized)
          .foreach { case (_, x) => isRemovedImportee += x }
      case x: RemoveImportee => isRemovedImportee += x.importee
      case _ =>
    }
    val importersToAdd = {
      val isAddedImporter = mutable.Set.empty[String]
      lazy val isAlreadyImported = {
        val isImported = mutable.Set.empty[Symbol]
        for { // register global imports
          import_ <- globalImports
          importer <- import_.importers
          importee <- importer.importees
          symbol <- index.symbol(importee).toList
          underlying <- SymbolOps.underlyingSymbols(symbol)
        } {
          isImported += underlying
        }
        isImported
      }
      importPatches.flatMap {
        case AddGlobalSymbol(symbol)
            if !allNamedImports.contains(symbol) &&
              !isAlreadyImported(symbol) &&
              !isPredef(symbol) =>
          isAlreadyImported += symbol
          SymbolOps.toImporter(symbol).toList
        case AddGlobalImport(importer)
            // best effort deduplication for syntactic addGlobalImport(Importer)
            if !allGlobalImportersSyntax.contains(importer.syntax) &&
              !isAddedImporter(importer.syntax) =>
          isAddedImporter += importer.syntax
          importer :: Nil
        case _ => Nil
      }
    }
    val grouped: Seq[Importer] =
      if (ctx.config.groupImportsByPrefix)
        importersToAdd
          .groupBy(_.ref.syntax)
          .map {
            case (_, is) =>
              Importer(
                is.head.ref,
                is.flatMap(_.importees)
                  .sortBy({
                    case Importee.Name(n) => n.value
                    case Importee.Rename(n, _) => n.value
                    case Importee.Unimport(n) => n.value
                    case Importee.Wildcard() => '\uFFFF'.toString
                  })
                  .toList
              )
          }
          .toList
      else importersToAdd
    val extraPatches =
      grouped
        .sortBy(_.ref.syntax)
        .map(is => ctx.addRight(editToken, s"\nimport ${is.syntax}"))
    val isRemovedImporter =
      allImporters.iterator
        .filter(_.importees.forall(isRemovedImportee))
        .toSet
    def removeSpaces(tokens: Iterable[Token]): Patch = {
      tokens
        .takeWhile {
          case Token.Space() => true
          case Newline() => true
          case Token.Comma() => true
          case _ => false
        }
        .map(ctx.removeToken)
        .asPatch
    }
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
                ctx.matchingParens
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
    val isRemovedImport =
      allImports.filter(_.importers.forall(isRemovedImporter))
    def remove(toRemove: Tree): Patch = {
      if (toRemove.pos == Position.None) return Patch.empty
      // Imagine "import a.b, c.d, e.f, g.h" where a.b, c.d and g.h are unused.
      // All unused imports are responible to delete their leading comma but
      // c.d is additionally responsible for deleting its trailling comma.
      // The same situation arises for importers: import a.{b, c, d, e} where
      // b, c and e are unused. In this case, the c import should delete it's
      // trailing comma.
      val isResponsibleForTrailingComma = toRemove.parent match {
        case Some(i: Import) =>
          val isSingleImporter = i.importers.lengthCompare(1) == 0
          !isSingleImporter &&
          i.importers
            .takeWhile(isRemovedImporter)
            .lastOption
            .exists(_ eq toRemove)
        case Some(i: Importer) =>
          val isSingleImportee = i.importees.lengthCompare(1) == 0
          !isSingleImportee &&
          i.importees
            .takeWhile(isRemovedImportee)
            .lastOption
            .exists(_ eq toRemove)
        case _ => false
      }
      // Always remove the leading comma
      val leadingComma =
        removeUpToFirstComma(ctx.tokenList.leading(toRemove.tokens.head))
      // Only remove the trailing comma for the first importer, for example
      // remove the trailing comma for a.b below because it's the first importer:
      // import a.b, c.d
      val trailingComma =
        if (isResponsibleForTrailingComma) {
          removeUpToFirstComma(ctx.tokenList.trailing(toRemove.tokens.last))
        } else {
          Nil
        }
      Patch.removeTokens(toRemove.tokens) ++ leadingComma ++ trailingComma
    }

    val leadingNewlines = isRemovedImport.map { i =>
      var newline = false
      ctx.tokenList
        .leading(ctx.toks(i).head)
        .takeWhile(x =>
          !newline && {
            x.is[Token.Space] || {
              val isNewline = x.is[Newline]
              if (isNewline) {
                newline = true
              }
              isNewline
            }
          }
        )
        .map(tok => ctx.removeToken(tok))
        .asPatch
    }

    leadingNewlines ++
      curlyBraceRemoves ++
      extraPatches ++
      isRemovedImportee.map(remove) ++
      isRemovedImporter.map(remove) ++
      isRemovedImport.map(i => Patch.removeTokens(i.tokens))
  }

  private def removeUpToFirstComma(tokens: Iterable[Token]): List[Patch] = {
    var foundComma = false
    val patch = tokens
      .takeWhile {
        case _: Token.Space | Newline() => true
        case _: Token.Comma if !foundComma =>
          foundComma = true
          true
        case _ => false
      }
      .map { token =>
        Patch.removeToken(token)
      }
      .toList
    if (foundComma) patch
    else Nil
  }

}
