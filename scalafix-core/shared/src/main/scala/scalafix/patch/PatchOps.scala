// scalafmt: {maxColumn = 100}
package scalafix
package patch

import scala.meta._
import org.scalameta.FileLine

object PatchOps {
  def removeTokens(tokens: Tokens): Patch =
    tokens.foldLeft(Patch.empty)(_ + TokenPatch.Remove(_))
}

trait SyntacticPatchOps {
  def removeImportee(importee: Importee): Patch
  def replaceToken(token: Token, toReplace: String): Patch
  def removeTokens(tokens: Tokens): Patch
  def removeToken(token: Token): Patch
  def rename(from: Name, to: Name)(implicit fileLine: FileLine): Patch
  def addRight(tok: Token, toAdd: String): Patch
  def addLeft(tok: Token, toAdd: String): Patch
}

trait SemanticPatchOps {
  def removeGlobalImport(symbol: Symbol)(implicit mirror: Mirror): Patch
  def addGlobalImport(importer: Importer)(implicit mirror: Mirror): Patch
  def replace(from: Symbol,
              to: Term.Ref,
              additionalImports: List[Importer] = Nil,
              normalized: Boolean = true)(implicit mirror: Mirror): Patch
  def renameSymbol(from: Symbol, to: Name, normalize: Boolean = true)(
      implicit mirror: Mirror): Patch
}
