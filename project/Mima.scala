// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/typesafehub/migration-manager/wiki/sbt-plugin#basic-usage
    Seq(
      // removed semanticdb-sbt
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.Versions.semanticdbSbt"),
      // @deprecated
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.Mirror"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.Rewrite"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.ScalafixConfig"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.RewriteCtx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.syntax.package#XtensionSymbolSemanticdbIndex.denotOpt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.syntax.package#XtensionRefSymbolOpt.symbolOpt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.sctx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.semanticCtx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.mirror"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleName.withOldName"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleName.generate"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleCtx.matching"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.Rule.andThen"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.SemanticRewrite"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.package$"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.Rewrite"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.package"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.lint.LintCategory.key"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.lint.LintMessage.format"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.patch.PatchOps.rename"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.Mirror"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.Rewrite"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.ScalafixConfig"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.package.RewriteCtx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.syntax.package#XtensionSymbolSemanticdbIndex.denotOpt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.syntax.package#XtensionRefSymbolOpt.symbolOpt"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.sctx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.semanticCtx"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.SemanticRule.mirror"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleName.withOldName"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleName.generate"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleCtx.matching"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.Rule.andThen"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.SemanticRewrite"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.package$"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.Rewrite"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.rewrite.package"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.lint.LintCategory.key"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.lint.LintMessage.format"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.patch.PatchOps.rename"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.testkit.SemanticRuleSuite.LintAssertion"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.testkit.package$"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.testkit.package"),
      // marked private[scalafix]
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.rule.RuleCtx.printLintMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.rule.RuleCtx.filter"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("scalafix.patch.Patch.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.patch.Patch.reportLintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.patch.Patch.lintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.rule.RuleCtx.printLintMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.rule.RuleCtx.filterLintMessage"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.cli.CliRunner.this"),
      ProblemFilters.exclude[FinalClassProblem]("scalafix.util.TokenList"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.util.TokenList.leadingSpaces"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.util.TokenList.trailingSpaces"),
      // Other
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.patch.PatchOps.replaceSymbols"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.patch.PatchOps.removeTokens"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*")
    )
  }
}
