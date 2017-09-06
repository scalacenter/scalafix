package banana.rewrite

import scala.meta._
import scala.meta.contrib._
import scalafix._

case class FqnRule(sctx: SemanticCtx) extends SemanticRewrite(sctx) {
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable")
}

case object FqnRule2 extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tree.collectFirst {
      case n: Name => ctx.replaceTree(n, n.value + "2")
    }.asPatch
}

object LambdaRewrites {
  val syntax: Rewrite = Rewrite.syntactic { ctx =>
    ctx.addLeft(ctx.tokens.head, "// comment\n")
  }

  val semantic = Rewrite.semantic { implicit sctx => ctx =>
    ctx.addGlobalImport(importer"scala.collection.mutable")
  }

}

case object PatchTokenWithEmptyRange extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tokens.collect {
      case tok @ Token.Interpolation.SpliceEnd() =>
        ctx.addRight(tok, "a")
      case tok @ Token.Xml.SpliceEnd() =>
        ctx.addRight(tok, "a")
    }
  }.asPatch
}
