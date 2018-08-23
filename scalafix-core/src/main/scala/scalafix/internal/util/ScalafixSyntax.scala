package scalafix.internal.util

trait ScalafixSyntax {

  type RuleName = scalafix.rule.RuleName
  val RuleName = scalafix.rule.RuleName

  type Patch = scalafix.patch.Patch
  val Patch = scalafix.patch.Patch

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }

  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }

  type LintCategory = scalafix.lint.LintCategory
  val LintCategory = scalafix.lint.LintCategory

  type Diagnostic = scalafix.lint.Diagnostic
  val Diagnostic = scalafix.lint.Diagnostic

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

}
