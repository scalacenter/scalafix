package scalafix
package rewrite

import scalafix.internal.rewrite._

object ScalafixRewrites {
  val syntax: List[Rule] = List(
    ProcedureSyntax,
    DottyVolatileLazyVal,
    RemoveXmlLiterals,
    ExplicitUnit,
    NoValInForComprehension,
    DottyKeywords,
    DottyVarArgPattern
  )
  def semantic(sctx: SemanticCtx): List[Rule] = List(
    Sbt1(sctx),
    ExplicitReturnTypes(sctx),
    RemoveUnusedImports(sctx),
    NoAutoTupling(sctx)
  )
  def all(sctx: SemanticCtx): List[Rule] =
    syntax ++ semantic(sctx)
  def name2rewrite(sctx: SemanticCtx): Map[String, Rule] =
    all(sctx).flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntaxName2rewrite: Map[String, Rule] =
    syntax.flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntacticNames: List[String] = syntaxName2rewrite.keys.toList
  lazy val semanticNames: List[String] =
    semantic(SemanticCtx.empty).flatMap(_.allNames)
  def allNames: List[String] = syntaxName2rewrite.keys.toList ++ semanticNames
}
