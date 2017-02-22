package scalafix.rewrite

import scala.collection.immutable.Seq

object ScalafixRewrites {
  val syntax: Seq[SyntaxRewrite] = Seq(
    ProcedureSyntax,
    VolatileLazyVal
  )
  val semantic: Seq[ScalafixRewrite] = Seq(
    ExplicitImplicit,
    Xor2Either
  )
  val all: Seq[ScalafixRewrite] = syntax ++ semantic
  val default: Seq[ScalafixRewrite] =
    all.filterNot(_ == VolatileLazyVal)
  val name2rewrite: Map[String, ScalafixRewrite] =
    all.map(x => x.name -> x).toMap

}
