package scalafix.internal.interfaces

import scala.meta.inputs.Position
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixMainCallback
import scalafix.internal.config
import scalafix.internal.config.ScalafixReporter
import scalafix.internal.util.LintSyntax
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintID
import scalafix.lint.LintSeverity

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
      override def lint(d: RuleDiagnostic): Unit = {
        val diagnostic = ScalafixDiagnosticImpl.fromScala(d)
        underlying.reportDiagnostic(diagnostic)
      }
      def report(msg: String, pos: Position, sev: LintSeverity): Unit = {
        val diagnostic = ScalafixDiagnosticImpl.fromScala(
          new LintSyntax.EagerRuleDiagnostic(msg, pos, sev, "", LintID.empty)
        )
        underlying.reportDiagnostic(diagnostic)
      }
    }

}
