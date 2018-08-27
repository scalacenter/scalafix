package scalafix.internal.interfaces

import scalafix.interfaces.ScalafixRule
import scalafix.v1

final class ScalafixRuleImpl(rule: v1.Rule) extends ScalafixRule {
  override def name(): String = rule.name.value
  override def description(): String = rule.description
  override def toString: String = s"ScalafixRule(${name()})"
}

object ScalafixRuleImpl {
  def apply(rule: v1.Rule): ScalafixRule = new ScalafixRuleImpl(rule)
}
