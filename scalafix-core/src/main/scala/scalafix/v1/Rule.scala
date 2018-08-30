package scalafix.v1

import metaconfig.Configured

abstract class Rule(val name: RuleName) {
  override def toString: String = name.toString
  def description: String = ""
  def isLinter: Boolean = false
  def isRewrite: Boolean = false
  def withConfiguration(config: Configuration): Configured[Rule] =
    Configured.ok(this)
}

abstract class SyntacticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: SyntacticDocument): Patch = Patch.empty
}

abstract class SemanticRule(name: RuleName) extends Rule(name) {
  def fix(implicit doc: SemanticDocument): Patch = Patch.empty
}

object SemanticRule {
  def constant(name: RuleName, patch: Patch): SemanticRule =
    new SemanticRule(name) {
      override def fix(implicit doc: SemanticDocument): Patch = patch
    }
}
