package banana.rewrite

import scala.meta._
import scala.meta.contrib._
import scalafix._

case class FqnRule(sctx: SemanticCtx) extends SemanticRewrite(sctx) {
  def name = "FqnRule"
  override def fix(ctx: RewriteCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable")
}

case object FqnRule2 extends Rule {
  def name = "FqnRule2"
  override def fix(ctx: RewriteCtx): Patch =
    ctx.tree.collectFirst {
      case n: Name => ctx.replaceTree(n, n.value + "2")
    }.asPatch
}

object LambdaRewrites {
  val syntax: Rewrite = Rewrite.syntactic("syntax") { ctx =>
    ctx.addLeft(ctx.tokens.head, "// comment\n")
  }

  val semantic = Rewrite.semantic("semantic") { implicit sctx => ctx =>
    ctx.addGlobalImport(importer"scala.collection.mutable")
  }

}

case object PatchTokenWithEmptyRange extends Rewrite {
  def name = "PatchTokenWithEmptyRange"
  override def fix(ctx: RewriteCtx): Patch = {
    ctx.tokens.collect {
      case tok @ Token.Interpolation.SpliceEnd() =>
        ctx.addRight(tok, "a")
      case tok @ Token.Xml.SpliceEnd() =>
        ctx.addRight(tok, "a")
    }
  }.asPatch
}
