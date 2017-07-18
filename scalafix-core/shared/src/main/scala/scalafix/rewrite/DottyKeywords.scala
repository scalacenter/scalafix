package scalafix
package rewrite

import scalafix.syntax._
import scala.meta._

case object DottyKeywords extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tree.collect {
      case name @ Name("enum") =>
        ctx.replaceTree(name, s"`enum`")
      case name @ Name("inline") if !name.parents.exists(_.is[Mod.Annot]) =>
        ctx.replaceTree(name, s"`inline`")
    }.asPatch
}
