// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      ProblemFilters.exclude[Problem]("scala.meta.internal.*"),
      // Exceptions
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.Versions.scala211")
    )
  }
}
