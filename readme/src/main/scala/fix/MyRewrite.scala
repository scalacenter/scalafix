package fix

import scala.meta.Name
import scalafix.Patch
import scalafix.Rule
import scalafix.rewrite.RewriteCtx

object MyRewrite {
  // Syntactic
  case object Uppercase extends Rule {
    override def rewrite(ctx: RewriteCtx): Patch =
      ctx.tree.collect {
        case tree @ Name(name) => ctx.replaceTree(tree, name.toUpperCase)
      }.asPatch
  }
}
