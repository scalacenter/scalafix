import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    Seq(
      exclude[MissingClassProblem]("scalafix.package$XtensionRewriteCtx")
    )
  }
}
