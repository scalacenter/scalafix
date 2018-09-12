package scalafix.v1

import metaconfig.Configured

abstract class Rule(val name: RuleName) {
  override def toString: String = name.toString

  /**
   * Single sentence explanation of what this rule does.
   *
   * Example: Removed unused imports reported by the compiler under -Ywarn-unused
   */
  def description: String = ""

  /**
   * Configure this rule according to user .scalafix.conf settings and compiler version.
   *
   * This method is called once per project/module. The same rule instance is used to
   * analyze multiple source files.
   *
   * @return A new version of this rule with loaded configuration or an error message.
   */
  def withConfiguration(config: Configuration): Configured[Rule] =
    Configured.ok(this)

  /** If true, allows this rule to be grouped together with other linter rules for documentation purposes. */
  def isLinter: Boolean = false
  /** If true, allows this rule to be grouped together with other rewrite rules for documentation purposes. */
  def isRewrite: Boolean = false
  /**
   * Indicates if this rule is incomplete and subject to breaking changes or removal in the future.
   *
   * Experimental rules are excluded from from tab completions and do not need to be documented
   * on the website.
   */
  def isExperimental: Boolean = false
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
