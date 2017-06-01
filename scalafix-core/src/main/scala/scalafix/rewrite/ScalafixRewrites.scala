package scalafix
package rewrite
import scala.meta._

object ScalafixRewrites {
  val syntax: List[Rewrite] = List(
    ProcedureSyntax,
    VolatileLazyVal,
    RemoveXmlLiterals,
    ExplicitUnit,
    NoValInForComprehension
  )
  def semantic(mirror: Mirror): List[Rewrite] = List(
    ExplicitReturnTypes(mirror),
    RemoveUnusedImports(mirror),
    Xor2Either(mirror),
    NoAutoTupling(mirror),
    NoUnusedImports(mirror)
  )
  def all(mirror: Mirror): List[Rewrite] =
    syntax ++ semantic(mirror)
  def default(mirror: Mirror): List[Rewrite] =
    all(mirror).filterNot(Set(VolatileLazyVal, Xor2Either))
  def name2rewrite(mirror: Mirror): Map[String, Rewrite] =
    all(mirror).map(x => x.name -> x).toMap
  lazy val syntaxName2rewrite: Map[String, Rewrite] =
    syntax.map(x => x.name -> x).toMap
  val emptyDatabase = Database(Nil)
  lazy val semanticNames: List[String] = semantic(emptyDatabase).map(_.name)
  def allNames: List[String] = syntaxName2rewrite.keys.toList ++ semanticNames
}
