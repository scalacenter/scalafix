// scalafmt: {maxColumn = 100}
package scalafix
package patch

import scala.meta._
import scalafix.patch.TokenPatch._
import scalafix.patch.TreePatch._

class SyntacticPatchOps(ctx: RewriteCtx) {
  def replaceToken(token: Token, toReplace: String): Patch =
    Add(token, "", toReplace, keepTok = false)
  def removeToken(token: Token): Patch = Add(token, "", "", keepTok = false)
  def rename(from: Name, to: Name): Patch = Rename(from, to)
  def addRight(tok: Token, toAdd: String): Patch = Add(tok, "", toAdd)
  def addLeft(tok: Token, toAdd: String): Patch = Add(tok, toAdd, "")
}

class SemanticPatchOps(ctx: RewriteCtx, mirror: Mirror) {
  def removeGlobalImport(importer: Importer): Patch =
    RemoveGlobalImport(importer)
  def addGlobalImport(importer: Importer): Patch =
    AddGlobalImport(importer)
  def replace(from: Symbol,
              to: Term.Ref,
              additionalImports: List[Importer] = Nil,
              normalized: Boolean = true): Patch =
    Replace(from, to, additionalImports, normalized)
  def renameSymbol(from: Symbol, to: Name, normalize: Boolean = false): Patch =
    RenameSymbol(from, to, normalize)
}
