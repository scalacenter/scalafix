// scalafmt: {maxColumn = 100}
package scalafix
package patch

import scala.meta._

trait PatchOps {
  // Syntactic patch ops.
  def removeImportee(importee: Importee): Patch
  def replaceToken(token: Token, toReplace: String): Patch
  def removeTokens(tokens: Tokens): Patch
  def removeToken(token: Token): Patch
  def replaceTree(from: Tree, to: String): Patch
  def rename(from: Name, to: Name): Patch
  def rename(from: Name, to: String): Patch
  def addRight(tok: Token, toAdd: String): Patch
  def addLeft(tok: Token, toAdd: String): Patch

  // Semantic patch ops.
  def moveSymbol(from: Symbol.Global, to: Symbol.Global)(implicit mirror: Mirror): Patch
  def removeGlobalImport(symbol: Symbol)(implicit mirror: Mirror): Patch
  def addGlobalImport(symbol: Symbol): Patch
  def addGlobalImport(importer: Importer)(implicit mirror: Mirror): Patch
  def replace(from: Symbol,
              to: Term.Ref,
              additionalImports: List[Importer] = Nil,
              normalized: Boolean = true)(implicit mirror: Mirror): Patch
  def renameSymbol(from: Symbol, to: Name, normalize: Boolean = true)(
      implicit mirror: Mirror): Patch
}
