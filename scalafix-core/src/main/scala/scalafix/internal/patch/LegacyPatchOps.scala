package scalafix.internal.patch

import scala.meta.Importee
import scala.meta.Importer
import scala.meta.Token
import scala.meta.Tree
import scala.meta.tokens.Tokens
import scalafix.internal.patch.LegacyPatchOps.DeprecationMessage
import scalafix.internal.util.SymbolOps.Root
import scalafix.patch.Patch.internal._
import scalafix.patch.PatchOps
import scalafix.v0.Symbol
import scalafix.v0._

trait LegacyPatchOps extends PatchOps {
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeImportee(importee: Importee): Patch =
    Patch.removeImportee(importee)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addGlobalImport(importer: Importer): Patch =
    Patch.addGlobalImport(importer)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceToken(token: Token, toReplace: String): Patch =
    Patch.replaceToken(token, toReplace)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeTokens(tokens: Tokens): Patch =
    Patch.removeTokens(tokens)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeTokens(tokens: Iterable[Token]): Patch =
    Patch.removeTokens(tokens)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeToken(token: Token): Patch =
    Patch.removeToken(token)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceTree(from: Tree, to: String): Patch =
    Patch.replaceTree(from, to)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addRight(tok: Token, toAdd: String): Patch =
    Patch.addRight(tok, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addRight(tree: Tree, toAdd: String): Patch =
    Patch.addRight(tree, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addLeft(tok: Token, toAdd: String): Patch =
    Patch.addLeft(tok, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addLeft(tree: Tree, toAdd: String): Patch =
    Patch.addLeft(tree, toAdd)

  // Semantic patch ops.
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeGlobalImport(
      symbol: Symbol
  )(implicit index: SemanticdbIndex): Patch =
    RemoveGlobalImport(symbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addGlobalImport(
      symbol: Symbol
  )(implicit index: SemanticdbIndex): Patch =
    AddGlobalSymbol(symbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit index: SemanticdbIndex
  ): Patch =
    ReplaceSymbol(fromSymbol, toSymbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbols(
      toReplace: (String, String)*
  )(implicit index: SemanticdbIndex): Patch =
    Patch.replaceSymbols(toReplace: _*)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbols(
      toReplace: scala.collection.Seq[(String, String)]
  )(implicit noop: DummyImplicit, index: SemanticdbIndex): Patch =
    Patch.replaceSymbols(toReplace.toSeq: _*)
  @deprecated(DeprecationMessage, "0.6.0")
  final def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit index: SemanticdbIndex
  ): Patch =
    ReplaceSymbol(fromSymbol, Root(Signature.Term(toName)))
  @deprecated(DeprecationMessage, "0.6.0")
  final def lint(msg: Diagnostic): Patch =
    Patch.lint(msg)
}

object LegacyPatchOps {
  private[scalafix] final val DeprecationMessage = "Use scalafix.Patch instead"
}
