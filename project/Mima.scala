// scalafmt: { maxColumn = 300 }
import com.typesafe.tools.mima.core._

object Mima {
  val ignoredABIProblems: Seq[ProblemFilter] = {
    // To learn more about mima, see:
    // See https://github.com/lightbend/mima
    Seq(
      ProblemFilters.exclude[Problem]("scalafix.internal.*"),
      ProblemFilters.exclude[Problem]("scala.meta.internal.*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("scalafix.interfaces.Scalafix.scala39"),
      // private[scalafix], so not callable by clients, but package-private in Scala compiles to a public method that MiMa still sees.
      // Its symbol table parameter moved from scala.meta.internal.symtab.SymbolTable to scalafix.internal.symtab.SymbolTable; both are internal types.
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("scalafix.testkit.RuleTest.fromPath")
    )
  }
}
