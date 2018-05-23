package scalafix.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.Patch
import scalafix.rule.RuleName

abstract class Rule(val name: RuleName) {
  override def toString: String = name.toString
  def description: String = ""
  def withConfig(conf: Conf): Configured[Rule] = Configured.ok(this)
}

abstract class SyntacticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: Doc): Patch = Patch.empty
}

abstract class SemanticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: SemanticDoc): Patch = Patch.empty
}
