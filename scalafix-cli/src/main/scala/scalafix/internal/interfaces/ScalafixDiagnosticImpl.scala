package scalafix.internal.interfaces
import java.util.Optional
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixLintID
import scalafix.interfaces.ScalafixPosition
import scalafix.interfaces.ScalafixSeverity
import scalafix.lint.LintID
import scalafix.lint.LintSeverity
import scalafix.lint.RuleDiagnostic

object ScalafixDiagnosticImpl {
  def fromScala(diagnostic: RuleDiagnostic): ScalafixDiagnostic =
    DelegateJavaDiagnostic(diagnostic)

  def fromJava(scalafixDiagnostic: ScalafixDiagnostic): RuleDiagnostic = {
    scalafixDiagnostic match {
      case DelegateJavaDiagnostic(underlying) =>
        underlying
      case _ =>
        throw new IllegalArgumentException(
          "Unexpected diagnostic: " + scalafixDiagnostic.toString)
    }
  }
}

case class DelegateJavaDiagnostic(diagnostic: RuleDiagnostic)
    extends ScalafixDiagnostic {
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
        override def categoryID(): String = diagnostic.id.category
      })
    }
}
