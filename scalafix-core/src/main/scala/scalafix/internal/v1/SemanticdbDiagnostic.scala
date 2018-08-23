package scalafix.internal.v1

import scala.meta.Position
import scala.meta.Input
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.Diagnostic.{Severity => d}

case class SemanticdbDiagnostic(input: Input, diagnostic: s.Diagnostic)
    extends Diagnostic {
  override def position: Position = diagnostic.range match {
    case Some(range) =>
      Position.Range(
        input,
        range.startLine,
        range.startCharacter,
        range.endLine,
        range.endCharacter
      )
    case None => Position.None
  }
  override def message: String = diagnostic.message
  override def severity: LintSeverity = diagnostic.severity match {
    case d.ERROR => LintSeverity.Error
    case d.WARNING => LintSeverity.Warning
    case d.INFORMATION => LintSeverity.Info
    case d.HINT | d.UNKNOWN_SEVERITY | _: d.Unrecognized => LintSeverity.Info
  }
}
