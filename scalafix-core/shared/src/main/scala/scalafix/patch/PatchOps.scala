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

  def removeGlobalImport(symbol: Symbol)(implicit mirror: Database): Patch
  def addGlobalImport(symbol: Symbol)(implicit mirror: Database): Patch
  def addGlobalImport(importer: Importer)(implicit mirror: Database): Patch
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit mirror: Database): Patch
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(implicit mirror: Database): Patch
}
