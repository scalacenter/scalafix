package scalafix.internal.interfaces

import scala.meta.inputs.Position
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixMainCallback
import scalafix.internal.config
import scalafix.internal.config.ScalafixReporter
import scalafix.lint.Diagnostic
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintID
import scalafix.lint.LintSeverity
import scalafix.rule.RuleName

object MainCallbackImpl {

  def default: ScalafixMainCallback = fromScala(config.ScalafixReporter.default)

  def fromScala(underlying: config.ScalafixReporter): ScalafixMainCallback =
    new ScalafixMainCallback {
      override def reportDiagnostic(d: ScalafixDiagnostic): Unit = {
        val diagnostic = ScalafixDiagnosticImpl.fromJava(d)
        if (diagnostic.id == LintID.empty) {
          underlying.report(
            diagnostic.message,
            diagnostic.position,
            diagnostic.severity)
        } else {
          underlying.lint(diagnostic)
        }
      }
    }

  def fromJava(underlying: ScalafixMainCallback): ScalafixReporter =
    new ScalafixReporter {
      override def lint(ruleDiagnostic: RuleDiagnostic): Unit = {
        val diagnostic = ScalafixDiagnosticImpl.fromScala(ruleDiagnostic)
        underlying.reportDiagnostic(diagnostic)
      }
      def report(msg: String, pos: Position, sev: LintSeverity): Unit = {
        val diagnostic = ScalafixDiagnosticImpl.fromScala(
          RuleDiagnostic(
            Diagnostic(id = "", msg, pos, "", sev),
            RuleName.empty,
            None)
        )
        underlying.reportDiagnostic(diagnostic)
      }
    }

}
