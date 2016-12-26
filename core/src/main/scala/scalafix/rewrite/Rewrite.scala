package scalafix.rewrite

import scala.meta._
import scalafix.Failure.MissingSemanticApi
import scalafix.util.Patch

abstract class Rewrite {
  def getSemanticApi(ctx: RewriteCtx): SemanticApi = ctx.semantic.getOrElse {
    throw MissingSemanticApi(this)
  }
  def rewrite(code: Tree, rewriteCtx: RewriteCtx): Seq[Patch]
}

object Rewrite {
  private def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val syntaxRewrites: Seq[Rewrite] = Seq(ProcedureSyntax, VolatileLazyVal)
  val semanticRewrites: Seq[Rewrite] = Seq(ExplicitImplicit, Xor2Either)
  val allRewrites: Seq[Rewrite] = syntaxRewrites ++ semanticRewrites
  val defaultRewrites: Seq[Rewrite] =
    allRewrites.filterNot(_ == VolatileLazyVal)
  val name2rewrite: Map[String, Rewrite] =
    allRewrites.map(x => x.toString -> x).toMap
}
