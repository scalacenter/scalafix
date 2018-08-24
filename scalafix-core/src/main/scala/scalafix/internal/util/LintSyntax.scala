package scalafix.internal.util

import scalafix.internal.config.ScalafixConfig
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintID
import scalafix.lint.Diagnostic
import scalafix.rule.RuleName

object LintSyntax {

  implicit class XtensionDiagnostic(msg: Diagnostic) {
    def fullStringID(name: RuleName): String =
      LintID(name.value, msg.categoryID).fullID

    def toDiagnostic(
        ruleName: RuleName,
        config: ScalafixConfig): RuleDiagnostic = {
      val id = LintID(ruleName.value, msg.categoryID)
      val configuredSeverity =
        config.lint.getConfiguredSeverity(id.fullID)
      RuleDiagnostic(msg, ruleName, configuredSeverity)
    }
  }

  implicit class XtensionRuleDiagnostic(msg: RuleDiagnostic) {
    def withMessage(newMessage: String): RuleDiagnostic = {
      RuleDiagnostic(
        Diagnostic(
          msg.id.category,
          newMessage,
          msg.position,
          msg.explanation,
          msg.severity
        ),
        msg.rule,
        msg.overriddenSeverity
      )
    }

  }
}
