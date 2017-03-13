package scalafix.rewrite

object ScalafixRewrites {
  val syntax: List[SyntaxRewrite] = List(
    ProcedureSyntax,
    VolatileLazyVal
  )
  val semantic: List[ScalafixRewrite] = List(
    ExplicitImplicit,
    Xor2Either
  )
  val all: List[ScalafixRewrite] = syntax ++ semantic
  val default: List[ScalafixRewrite] =
    all.filterNot(Set(VolatileLazyVal, Xor2Either))
  val name2rewrite: Map[String, ScalafixRewrite] =
    all.map(x => x.name -> x).toMap


}
