import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // After v0.5, start running mima checks in CI and document breaking changes here.
    // See https://github.com/typesafehub/migration-manager/wiki/sbt-plugin#basic-usage
    Seq(
      ProblemFilters.exclude[Problem](
        "scalafix.testkit.DiffAssertion*"),
      ProblemFilters.exclude[Problem](
        "scalafix.testkit.SemanticRuleSuite*"),
      ProblemFilters.exclude[Problem](
        "scalafix.testkit.SyntacticRuleSuite*"),
      ProblemFilters.exclude[MissingClassProblem](
        "scalafix.tests.IntegrationPropertyTest*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.replaceSymbols"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.removeTokens"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*")
    )
  }
}
