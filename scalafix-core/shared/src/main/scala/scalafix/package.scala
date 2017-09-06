package object scalafix {

  type SemanticCtx = scalafix.util.SemanticCtx
  val SemanticCtx = scalafix.util.SemanticCtx

  // These are remnants from scala.meta.SemanticCtx which got removed in 2.0.0-M2.
  // and kept here for compatibility with existing rules.
  @deprecated("Renamed to SemanticCtx", "0.5.0")
  type Mirror = scalafix.util.SemanticCtx
  @deprecated("Renamed to SemanticCtx", "0.5.0")
  val Mirror = scalafix.util.SemanticCtx

  @deprecated(
    "ScalafixConfig is now internal, import scalafix.internal.config.ScalafixConfig instead.",
    "0.5.0")
  type ScalafixConfig = internal.config.ScalafixConfig
  @deprecated(
    "ScalafixConfig is now internal, import scalafix.internal.config.ScalafixConfig instead.",
    "0.5.0")
  val ScalafixConfig = internal.config.ScalafixConfig

  type RuleCtx = rule.RuleCtx
  val RuleCtx = rule.RuleCtx

  @deprecated("Renamed to RuleCtx", "0.5.0")
  type RewriteCtx = rule.RuleCtx
  @deprecated("Renamed to RuleCtx", "0.5.0")
  val RewriteCtx = rule.RuleCtx

  @deprecated("Renamed to SemanticRule", "0.5.0")
  type SemanticRewrite = rule.SemanticRule
  @deprecated("Renamed to Rule", "0.5.0")
  type Rewrite = rule.Rule
  @deprecated("Renamed to Rule", "0.5.0")
  val Rewrite = rule.Rule

  type SemanticRule = rule.SemanticRule
  type Rule = rule.Rule
  val Rule = rule.Rule

  type Patch = patch.Patch
  val Patch = patch.Patch

  type LintCategory = scalafix.lint.LintCategory
  val LintCategory = scalafix.lint.LintCategory

  type LintMessage = scalafix.lint.LintMessage

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }
}
