package scalafix.internal.interfaces
import java.util.Optional
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixLintID
import scalafix.interfaces.ScalafixPosition
import scalafix.interfaces.ScalafixSeverity
import scalafix.lint.LintDiagnostic
import scalafix.lint.LintID
import scalafix.lint.LintSeverity

object ScalafixDiagnosticImpl {
  def fromScala(diagnostic: LintDiagnostic): ScalafixDiagnostic =
    new ScalafixDiagnostic {
      override def severity(): ScalafixSeverity = diagnostic.severity match {
        case LintSeverity.Info => ScalafixSeverity.INFO
        case LintSeverity.Warning => ScalafixSeverity.WARNING
        case LintSeverity.Error => ScalafixSeverity.ERROR
      }
      override def message(): String = diagnostic.message
      override def explanation(): String = diagnostic.explanation
      override def position(): Optional[ScalafixPosition] =
        PositionImpl.optionalFromScala(diagnostic.position)
      override def lintID(): Optional[ScalafixLintID] =
        if (diagnostic.id == LintID.empty) {
          Optional.empty()
        } else {
          Optional.of(new ScalafixLintID {
            override def ruleName(): String = diagnostic.id.rule
            override def categoryID(): String = diagnostic.id.categoryID
          })
        }
    }

  def fromJava(diagnostic: ScalafixDiagnostic): LintDiagnostic =
    new LintDiagnostic {
      override def message: String = diagnostic.message()
      override def position: Position = {
        if (diagnostic.position().isPresent) {
          val spos = diagnostic.position().get
          val input = Input.VirtualFile(
            spos.input().filename(),
            spos.input().text().toString
          )
          Position.Range(input, spos.startOffset(), spos.endOffset())
        } else {
          Position.None
        }
      }
      override def severity: LintSeverity = diagnostic.severity() match {
        case ScalafixSeverity.INFO => LintSeverity.Info
        case ScalafixSeverity.WARNING => LintSeverity.Warning
        case ScalafixSeverity.ERROR => LintSeverity.Error
      }
      override def explanation: String = diagnostic.explanation()
      override def id: LintID = {
        if (diagnostic.lintID().isPresent) {
          val lintID = diagnostic.lintID().get
          LintID(lintID.ruleName(), lintID.categoryID())
        } else {
          LintID.empty
        }
      }
    }
}
