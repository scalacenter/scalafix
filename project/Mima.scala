// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[MissingTypesProblem]("scalafix.testkit.DiffAssertions"),
      ProblemFilters.exclude[MissingTypesProblem]("scalafix.testkit.SemanticRuleSuite"),
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.ScalafixArguments.evaluate"),
      // https://github.com/sbt/sbt-buildinfo/pull/150
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("scalafix.Versions.supportedScalaVersions"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("buildinfo.RulesBuildInfo.allSupportedScalaVersions"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("buildinfo.RulesBuildInfo.supportedScalaVersions"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.ScalafixFileEvaluation.getErrorMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.ScalafixEvaluation.getError"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.ScalafixEvaluation.getErrorMessage"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.ScalafixFileEvaluation.getError"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("scalafix.v1.SemanticDocument.fromPath"),
      ProblemFilters.exclude[Problem]("scala.meta.internal.*"),
      ProblemFilters.exclude[Problem]("scala.tools.nsc.interactive.*"),
      ProblemFilters.exclude[Problem]("scalafix.syntax.package.XtensionDocument"),
      ProblemFilters.exclude[MissingClassProblem]("scalafix.syntax.package$XtensionDocument")
    )
  }
}
