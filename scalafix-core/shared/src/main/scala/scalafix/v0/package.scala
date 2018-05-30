package scalafix

package object v0 extends scalafix.internal.util.ScalafixSyntax {

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

  type LintCategory = scalafix.lint.LintCategory
  val LintCategory = scalafix.lint.LintCategory

  type LintMessage = scalafix.lint.LintMessage

}
