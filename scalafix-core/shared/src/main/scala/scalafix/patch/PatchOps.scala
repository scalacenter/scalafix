// scalafmt: {maxColumn = 100}
package scalafix
package patch

import scala.meta._

trait PatchOps {
  def removeImportee(importee: Importee): Patch
  def replaceToken(token: Token, toReplace: String): Patch
  def removeTokens(tokens: Tokens): Patch
  def removeToken(token: Token): Patch
  def replaceTree(from: Tree, to: String): Patch
  def rename(from: Name, to: Name): Patch
  def rename(from: Name, to: String): Patch
  def addRight(tok: Token, toAdd: String): Patch
  def addLeft(tok: Token, toAdd: String): Patch
  def lint(msg: LintMessage): Patch

  def removeGlobalImport(symbol: Symbol)(implicit semanticCtx: SemanticCtx): Patch
  def addGlobalImport(symbol: Symbol)(implicit semanticCtx: SemanticCtx): Patch
  def addGlobalImport(importer: Importer)(implicit semanticCtx: SemanticCtx): Patch
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit semanticCtx: SemanticCtx): Patch
  def replaceSymbols(toReplace: (String, String)*)(implicit semanticCtx: SemanticCtx): Patch
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit semanticCtx: SemanticCtx): Patch
}
