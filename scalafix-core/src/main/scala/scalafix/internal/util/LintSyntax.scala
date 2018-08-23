package scalafix.internal.util

import scalafix.internal.config.ScalafixConfig
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintID
import scalafix.lint.Diagnostic
import scalafix.rule.RuleName
import scala.meta.inputs.Position
import scalafix.lint.LintSeverity

object LintSyntax {

  implicit class XtensionDiagnostic(msg: Diagnostic) {
    def fullStringID(name: RuleName): String =
      LintID(name.value, msg.categoryID).fullStringID

    def toDiagnostic(
        ruleName: RuleName,
        config: ScalafixConfig): RuleDiagnostic = {
      val id = LintID(ruleName.value, msg.categoryID)
      RuleDiagnostic(
        msg,
        ruleName,
        config.lint.getConfiguredSeverity(id.fullStringID)
      )
    }
  }

  implicit class XtensionRuleDiagnostic(msg: RuleDiagnostic) {
    def withMessage(newMessage: String): RuleDiagnostic = {
      new EagerRuleDiagnostic(
        newMessage,
        msg.position,
        msg.severity,
        msg.explanation,
        msg.id
      )
    }

  }
  final class EagerRuleDiagnostic(
      val message: String,
      val position: Position,
      val severity: LintSeverity,
      val explanation: String,
      val id: LintID
  ) extends RuleDiagnostic {
    override def toString: String = formattedMessage
  }

}
