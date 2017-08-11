package fix

import scala.meta.Name
import scalafix.Patch
import scalafix.Rewrite
import scalafix.rewrite.RewriteCtx

object MyRewrite {
  // Syntactic "lambda-rewrite"
  val Reverse: Rewrite = Rewrite.syntactic { ctx =>
    ctx.tree.collect {
      case tree @ Name(name) => ctx.replaceTree(tree, name.reverse)
    }.asPatch
  }
  // Syntactic "class-rewrite"
  case object Uppercase extends Rewrite {
    override def rewrite(ctx: RewriteCtx): Patch =
      ctx.tree.collect {
        case tree @ Name(name) => ctx.replaceTree(tree, name.toUpperCase)
      }.asPatch
  }
}
