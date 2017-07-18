package scalafix
package rewrite

import scala.meta._

case object DottyKeywords extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tree.collect {
      case name @ Name("enum" | "inline") =>
        ctx.replaceTree(name, s"`${name.value}`")
    }.asPatch
}
