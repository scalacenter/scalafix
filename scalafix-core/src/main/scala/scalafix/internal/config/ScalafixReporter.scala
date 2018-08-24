package scalafix.internal.config

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import scala.meta.Position
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintSeverity

trait ScalafixReporter {
  private[scalafix] def report(
      message: String,
      position: Position,
      severity: LintSeverity): Unit
  private[scalafix] def lint(d: RuleDiagnostic): Unit
  final def info(message: String, position: Position = Position.None): Unit =
    report(message, position, LintSeverity.Info)
  final def warn(message: String, position: Position = Position.None): Unit =
    report(message, position, LintSeverity.Warning)
  final def error(message: String, position: Position = Position.None): Unit =
    report(message, position, LintSeverity.Error)
}

object ScalafixReporter {
  def default: ScalafixReporter = PrintStreamReporter.default
  implicit val decoder: ConfDecoder[ScalafixReporter] =
    ConfDecoder.stringConfDecoder.map(_ => default)
  implicit val encoder: ConfEncoder[ScalafixReporter] =
    ConfEncoder.StringEncoder.contramap(_ => "<reporter>")
}
