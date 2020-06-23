// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[MissingTypesProblem]("scalafix.testkit.DiffAssertions"),
      ProblemFilters.exclude[MissingTypesProblem]("scalafix.testkit.SemanticRuleSuite"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*")
    )
  }
}
