package object scalafix {

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

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }

  private[scalafix] implicit class XtensionString(val value: String)
      extends AnyVal {
    def isMultiline: Boolean =
      value.indexOf('\n') != -1
  }
}
