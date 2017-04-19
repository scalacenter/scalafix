package scalafix
package rewrite

object ScalafixRewrites {
  val syntax: List[Rewrite] = List(
    ProcedureSyntax,
    VolatileLazyVal
  )
  def semantic(mirror: ScalafixMirror): List[Rewrite] = List(
    ScalaJsRewrites.DemandJSGlobal(mirror),
    ExplicitImplicit()(mirror),
    Scalameta17()(mirror),
    Xor2Either()(mirror)
  )
  def all(mirror: ScalafixMirror): List[Rewrite] =
    syntax ++ semantic(mirror)
  def default(mirror: ScalafixMirror): List[Rewrite] =
    all(mirror).filterNot(Set(VolatileLazyVal, Xor2Either))
  def defaultName: List[String] =
    default(null).map(_.name) // TODO(olafur) ugly hack
  def name2rewrite(mirror: ScalafixMirror): Map[String, Rewrite] =
    all(mirror).map(x => x.name -> x).toMap
  lazy val syntaxName2rewrite: Map[String, Rewrite] =
    syntax.map(x => x.name -> x).toMap

}
