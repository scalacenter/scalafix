package scalafix
package rewrite
import scala.meta._

object ScalafixRewrites {
  val syntax: List[Rewrite] = List(
    ProcedureSyntax,
    VolatileLazyVal
  )
  def semantic(mirror: Mirror): List[Rewrite] = List(
    ScalaJsRewrites.DemandJSGlobal(mirror),
//    ExplicitImplicit(mirror), // Unsupported for now
    Scalameta17(mirror),
    Xor2Either(mirror)
  )
  def all(mirror: Mirror): List[Rewrite] =
    syntax ++ semantic(mirror)
  def default(mirror: Mirror): List[Rewrite] =
    all(mirror).filterNot(Set(VolatileLazyVal, Xor2Either))
  def name2rewrite(mirror: Mirror): Map[String, Rewrite] =
    all(mirror).map(x => x.name -> x).toMap
  lazy val syntaxName2rewrite: Map[String, Rewrite] =
    syntax.map(x => x.name -> x).toMap
}
