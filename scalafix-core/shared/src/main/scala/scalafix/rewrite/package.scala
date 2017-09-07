package scalafix

package object rewrite {
  @deprecated("Renamed to RuleCtx", "0.5.0")
  type RewriteCtx = RuleCtx
  @deprecated("Renamed to RuleCtx", "0.5.0")
  type SemanticCtx = RuleCtx
  @deprecated("Renamed to RuleCtx", "0.5.0")
  val SemanticCtx = RuleCtx
  @deprecated("Renamed to RuleName", "0.5.0")
  type RewriteName = rule.RuleName
  @deprecated("Renamed to RuleName", "0.5.0")
  val RewriteName = rule.RuleName
}
