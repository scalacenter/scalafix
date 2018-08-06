package scalafix.internal.util

import scalafix.internal.config.ScalafixConfig
import scalafix.lint.LintDiagnostic
import scalafix.lint.LintID
import scalafix.lint.LintMessage
import scalafix.rule.RuleName
import scala.meta.inputs.Position
import scalafix.lint.LintSeverity

object LintSyntax {

  implicit class XtensionLintMessage(msg: LintMessage) {
    def fullStringID(name: RuleName): String =
      LintID(name.value, msg.categoryID).fullStringID

    def toDiagnostic(
        ruleName: RuleName,
        config: ScalafixConfig): LintDiagnostic = {
      val id = LintID(ruleName.value, msg.categoryID)
      LintDiagnostic(
        msg,
        ruleName,
        config.lint.getConfiguredSeverity(id.fullStringID)
      )
    }
  }

  implicit class XtensionLintDiagnostic(msg: LintDiagnostic) {
    def withMessage(newMessage: String): LintDiagnostic = {
      new EagerLintDiagnostic(
        newMessage,
        msg.position,
        msg.severity,
        msg.explanation,
        msg.id
      )
    }

  }
  final class EagerLintDiagnostic(
      val message: String,
      val position: Position,
      val severity: LintSeverity,
      val explanation: String,
      val id: LintID
  ) extends LintDiagnostic {
    override def toString: String = formattedMessage
  }

}
