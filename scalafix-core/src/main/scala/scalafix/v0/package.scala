package scalafix

package object v0 extends scalafix.internal.util.ScalafixSyntax {
  @deprecated("Use Diagnostic instead", "0.6.0")
  type LintMessage = Diagnostic
  @deprecated("Use Diagnostic instead", "0.6.0")
  val LintMessage = Diagnostic
}
