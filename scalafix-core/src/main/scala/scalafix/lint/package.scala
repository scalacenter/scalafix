package scalafix

package object lint {

  @deprecated("Use Diagnostic instead", "0.6.0")
  type LintMessage = scalafix.v0.LintMessage
  @deprecated("Use Diagnostic instead", "0.6.0")
  val LintMessage = scalafix.v0.LintMessage

  @deprecated("Use Diagnostic instead", "0.6.0")
  type LintCategory = scalafix.v0.LintCategory
  @deprecated("Use Diagnostic instead", "0.6.0")
  val LintCategory = scalafix.v0.LintCategory
}
