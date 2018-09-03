package scalafix.internal.interfaces

import scalafix.interfaces.ScalafixRule
import scalafix.interfaces.ScalafixRuleKind
import scalafix.v1

final class ScalafixRuleImpl(rule: v1.Rule) extends ScalafixRule {
  override def name(): String = rule.name.value
  override def description(): String = rule.description
  override def toString: String = s"ScalafixRule(${name()})"
  override def kind(): ScalafixRuleKind = rule match {
    case _: v1.SemanticRule => ScalafixRuleKind.SEMANTIC
    case _: v1.SyntacticRule => ScalafixRuleKind.SYNTACTIC
    case _ =>
      throw new IllegalArgumentException(
        s"Rule '$rule' is neither semantic or syntactic")
  }
  override def isLinter: Boolean = rule.isLinter
  override def isRewrite: Boolean = rule.isRewrite
}

object ScalafixRuleImpl {
  def apply(rule: v1.Rule): ScalafixRule = new ScalafixRuleImpl(rule)
}
