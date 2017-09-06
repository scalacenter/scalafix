package scalafix
package rule

import scalafix.internal.rule._

object ScalafixRules {
  val syntax: List[Rule] = List(
    ProcedureSyntax,
    DottyVolatileLazyVal,
    RemoveXmlLiterals,
    ExplicitUnit,
    NoValInForComprehension,
    DottyKeywords,
    DottyVarArgPattern
  )
  def semantic(sctx: SemanticdbIndex): List[Rule] = List(
    Sbt1(sctx),
    ExplicitResultTypes(sctx),
    RemoveUnusedImports(sctx),
    NoAutoTupling(sctx)
  )
  def all(sctx: SemanticdbIndex): List[Rule] =
    syntax ++ semantic(sctx)
  def name2rule(sctx: SemanticdbIndex): Map[String, Rule] =
    all(sctx).flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntaxName2rule: Map[String, Rule] =
    syntax.flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntacticNames: List[String] = syntaxName2rule.keys.toList
  lazy val semanticNames: List[String] =
    semantic(SemanticdbIndex.empty).flatMap(_.allNames)
  def allNames: List[String] = syntaxName2rule.keys.toList ++ semanticNames
}
