// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      ProblemFilters.exclude[Problem]("scala.meta.internal.*"),
      // deprecation of TestRegistration, switch to AnyFunSuite; remove after next release
      ProblemFilters.exclude[FinalMethodProblem]("org.scalatest.funsuite.AnyFunSuite.styleName"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("scalafix.testkit.AbstractSemanticRuleSuite.*"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("scalafix.testkit.AbstractSyntacticRuleSuite.*"),
      // Exceptions
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.Scalafix.scala38")
    )
  }
}
