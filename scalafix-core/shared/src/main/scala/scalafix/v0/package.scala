package scalafix

package object v0 {
  type SemanticdbIndex = scalafix.util.SemanticdbIndex
  val SemanticdbIndex = scalafix.util.SemanticdbIndex

  type RuleCtx = rule.RuleCtx
  val RuleCtx = rule.RuleCtx

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

  type SemanticRule = rule.SemanticRule
  type Rule = rule.Rule
  val Rule = rule.Rule

  type Patch = patch.Patch
  val Patch = patch.Patch

  type LintCategory = scalafix.lint.LintCategory
  val LintCategory = scalafix.lint.LintCategory

  type LintMessage = scalafix.lint.LintMessage

}
