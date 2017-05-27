package scalafix
package rewrite

import scala.meta._
import scalafix.syntax._
import org.scalameta.logger

case class RemoveUnusedImports(mirror: Mirror)
    extends SemanticRewrite(mirror) {
  private val unusedImports = mirror.database.messages.toIterator.collect {
    case Message(pos, _, "Unused import") =>
      pos
  }.toSet
  private def isUnused(importee: Importee) = importee match {
    // NOTE: workaround for https://github.com/scalameta/scalameta/issues/899
    case Importee.Wildcard() =>
      val lookupPos =
        importee.parents
          .collectFirst { case x: Import => x.pos }
          .getOrElse(importee.pos)
      unusedImports.contains(lookupPos)
    case _ => unusedImports.contains(importee.pos)
  }
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tree.collect {
      case i: Importee if isUnused(i) => ctx.removeImportee(i)
    }.asPatch
}
