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
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.patch.Patch.reportLintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.patch.Patch.lintMessages"),
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "scalafix.rule.RuleCtx.printLintMessage")
    )
  }
}
