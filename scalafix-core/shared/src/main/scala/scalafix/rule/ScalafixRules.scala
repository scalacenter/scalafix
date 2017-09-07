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
  def semantic(index: SemanticdbIndex): List[Rule] = List(
    NoInfer(index),
    Sbt1(index),
    ExplicitResultTypes(index),
    RemoveUnusedImports(index),
    NoAutoTupling(index)
  )
  def all(index: SemanticdbIndex): List[Rule] =
    syntax ++ semantic(index)
  def name2rule(index: SemanticdbIndex): Map[String, Rule] =
    all(index).flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntaxName2rule: Map[String, Rule] =
    syntax.flatMap(x => x.allNames.map(_ -> x)).toMap
  lazy val syntacticNames: List[String] = syntaxName2rule.keys.toList
  lazy val semanticNames: List[String] =
    semantic(SemanticdbIndex.empty).flatMap(_.allNames)
  def allNames: List[String] = syntaxName2rule.keys.toList ++ semanticNames
}
