// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      ProblemFilters.exclude[Problem]("scala.meta.internal.*"),
      // Scala-private constructor: only the companion, compiled and shipped in the same artifact, links against it
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.v1.RuleDecoder#Settings.this")
    )
  }
}
