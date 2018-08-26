package scalafix.v1

import metaconfig.Configured

abstract class Rule(val name: RuleName) {
  override def toString: String = name.toString
  def description: String = ""
  def withConfiguration(config: Configuration): Configured[Rule] =
    Configured.ok(this)
}

abstract class SyntacticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: Doc): Patch = Patch.empty
}

abstract class SemanticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: SemanticDoc): Patch = Patch.empty
}

object SemanticRule {
  def constant(name: RuleName, patch: Patch): SemanticRule =
    new SemanticRule(name) {
      override def fix(implicit doc: SemanticDoc): Patch = patch
    }
}
