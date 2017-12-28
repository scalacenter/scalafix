import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // After v0.5, start running mima checks in CI and document breaking changes here.
    // See https://github.com/typesafehub/migration-manager/wiki/sbt-plugin#basic-usage
    Seq(
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.replaceSymbols"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.removeTokens"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      // marked private[scalafix]
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.rule.RuleCtx.printLintMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.rule.RuleCtx.filter"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem](
        "scalafix.patch.Patch.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.patch.Patch.reportLintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.patch.Patch.lintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.rule.RuleCtx.printLintMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.rule.RuleCtx.filterLintMessage"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.cli.CliRunner.this"),
      ProblemFilters.exclude[FinalClassProblem]("scalafix.util.TokenList"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.util.TokenList.leadingSpaces"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.util.TokenList.trailingSpaces"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem](
        "scalafix.util.TokenList.slice")
    )
  }
}
