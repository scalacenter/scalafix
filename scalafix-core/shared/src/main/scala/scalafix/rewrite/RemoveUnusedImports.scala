package scalafix
package rewrite

import scala.meta._
import scalafix.syntax._
import org.scalameta.logger

case class RemoveUnusedImports(mirror: Database)
    extends SemanticRewrite(mirror) {
  private val unusedImports = mirror.database.messages.toIterator.collect {
    case Message(pos, _, "Unused import") =>
      pos
  }.toSet
  private def isUnused(importee: Importee) = {
    val pos = importee match {
      // NOTE: workaround for https://github.com/scalameta/scalameta/issues/899
      case Importee.Wildcard() =>
        importee.parents
          .collectFirst { case x: Import => x.pos }
          .getOrElse(importee.pos)
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
