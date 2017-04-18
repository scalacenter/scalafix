package scalafix
package rewrite
import scala.meta._
import scalafix.util.TreePatch._
import scalafix.util._

trait SyntacticPatchOps[T] {
  implicit def ctx: RewriteCtx[T]
  def rename(from: Name, to: Name): Patch = Rename(from, to)
}

trait SemanticPatchOps extends SyntacticPatchOps[Mirror] {
  def addGlobalImport(importer: Importer): Patch = AddGlobalImport(importer)
}
