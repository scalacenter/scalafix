package scalafix
package rewrite
import scala.meta._

object ScalafixRewrites {
  val syntax: List[Rewrite] = List(
    ProcedureSyntax,
    VolatileLazyVal,
    RemoveXmlLiterals,
    ExplicitUnit,
    NoValInForComprehension,
    DottyKeywords,
    DottyVarArgPattern
  )
  def semantic(mirror: SemanticCtx): List[Rewrite] = List(
    ExplicitReturnTypes(mirror),
    RemoveUnusedImports(mirror),
    NoAutoTupling(mirror)
  )
  def all(mirror: SemanticCtx): List[Rewrite] =
    syntax ++ semantic(mirror)
  def name2rewrite(mirror: SemanticCtx): Map[String, Rewrite] =
    all(mirror).map(x => x.name -> x).toMap
  lazy val syntaxName2rewrite: Map[String, Rewrite] =
    syntax.map(x => x.name -> x).toMap
  val emptyDatabase = SemanticCtx(Nil)
  lazy val syntacticNames: List[String] = syntaxName2rewrite.keys.toList
  lazy val semanticNames: List[String] = semantic(emptyDatabase).map(_.name)
  def allNames: List[String] = syntaxName2rewrite.keys.toList ++ semanticNames
}
