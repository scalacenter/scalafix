package scalafix.internal.v0

import scalafix.internal.config.DisableConfig
import scalafix.internal.rule.Disable
import scalafix.internal.rule.DottyKeywords
import scalafix.internal.rule.DottyVarArgPattern
import scalafix.internal.rule.DottyVolatileLazyVal
import scalafix.internal.rule.ExplicitResultTypes
import scalafix.internal.rule.MissingFinal
import scalafix.internal.rule.NoValInForComprehension
import scalafix.rule.Rule
import scalafix.util.SemanticdbIndex

object LegacyRules {
  val syntax: List[Rule] = List(
    DottyVolatileLazyVal,
    NoValInForComprehension,
    DottyKeywords,
    DottyVarArgPattern
  )
  def semantic(index: SemanticdbIndex): List[Rule] = List(
    ExplicitResultTypes(index),
    Disable(index, DisableConfig.default),
    MissingFinal(index)
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
  def allNames: List[String] = syntacticNames ++ semanticNames

  lazy val syntacticNamesDescriptions: List[(String, String)] =
    syntaxName2rule.toList.map { case (name, rule) => (name, rule.description) }
  lazy val semanticNamesDescriptions: List[(String, String)] =
    semantic(SemanticdbIndex.empty).flatMap(rule =>
      rule.allNames.map(name => (name, rule.description)))
  def allNamesDescriptions: List[(String, String)] =
    syntacticNamesDescriptions ++ semanticNamesDescriptions

}
