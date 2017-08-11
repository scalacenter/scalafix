package scalafix
package rewrite

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
  def semantic(semanticCtx: SemanticCtx): List[Rewrite] = List(
    ExplicitReturnTypes(semanticCtx),
    RemoveUnusedImports(semanticCtx),
    NoAutoTupling(semanticCtx)
  )
  def all(semanticCtx: SemanticCtx): List[Rewrite] =
    syntax ++ semantic(semanticCtx)
  def name2rewrite(semanticCtx: SemanticCtx): Map[String, Rewrite] =
    all(semanticCtx).map(x => x.name -> x).toMap
  lazy val syntaxName2rewrite: Map[String, Rewrite] =
    syntax.map(x => x.name -> x).toMap
  val emptyDatabase = SemanticCtx(Nil)
  lazy val syntacticNames: List[String] = syntaxName2rewrite.keys.toList
  lazy val semanticNames: List[String] = semantic(emptyDatabase).map(_.name)
  def allNames: List[String] = syntaxName2rewrite.keys.toList ++ semanticNames
}
