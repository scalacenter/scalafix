package scalafix

package object lint {
  @deprecated("Use Diagnostic instead", "0.6.0")
  type LintMessage = scalafix.lint.Diagnostic
  @deprecated("Use Diagnostic instead", "0.6.0")
  val LintMessage = scalafix.lint.Diagnostic
}
