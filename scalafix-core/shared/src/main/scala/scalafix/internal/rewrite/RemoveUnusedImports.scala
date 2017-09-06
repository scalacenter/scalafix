package scalafix.internal.rewrite

import scala.meta._
import scalafix.Patch
import scalafix.SemanticCtx
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.SemanticRule

case class RemoveUnusedImports(sctx: SemanticCtx) extends SemanticRule(sctx) {
  private val unusedImports = sctx.messages.toIterator.collect {
    case Message(pos, _, "Unused import") =>
      pos
  }.toSet
  private def isUnused(importee: Importee) = {
    val pos = importee match {
      case Importee.Rename(name, _) => name.pos
      case _ => importee.pos
    }
    unusedImports.contains(pos)
  }
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tree.collect {
      case i: Importee if isUnused(i) => ctx.removeImportee(i)
    }.asPatch
}
