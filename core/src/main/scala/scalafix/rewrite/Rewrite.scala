package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.util.Patch

abstract class Rewrite[T](implicit sourceName: sourcecode.Name) {
  def name: String = sourceName.value
  def rewrite(ctx: RewriteCtx[T]): Seq[Patch]
}

object Rewrite {
  private def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val syntaxRewrites: Seq[ScalafixRewrite] = Seq(
    ProcedureSyntax.default,
    VolatileLazyVal.default
  )
  val semanticRewrites: Seq[ScalafixRewrite] = Seq(
    ExplicitImplicit,
    Xor2Either
  )
  val allRewrites: Seq[ScalafixRewrite] = syntaxRewrites ++ semanticRewrites
  val defaultRewrites: Seq[ScalafixRewrite] =
    allRewrites.filterNot(_ == VolatileLazyVal.default)
  val name2rewrite: Map[String, ScalafixRewrite] =
    allRewrites.map(x => x.name -> x).toMap
}
