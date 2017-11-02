import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // After v0.5, start running mima checks in CI and document breaking changes here.
    // See https://github.com/typesafehub/migration-manager/wiki/sbt-plugin#basic-usage
    Seq(
      // Ignore all testkit.* problems due to utest change.
      ProblemFilters.exclude[Problem]("scalafix.testkit.*"),
      ProblemFilters.exclude[Problem]("scalafix.tests.*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.replaceSymbols"),
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "scalafix.patch.PatchOps.removeTokens"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*")
    )
  }
}
