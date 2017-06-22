package scalafix.cli

sealed abstract class CliCommand {
  import CliCommand._
  def isOk: Boolean = !isError
  def isError: Boolean = this match {
    case RunScalafix(_) => false
    case PrintAndExit(_, ExitStatus(code, _)) => code != 0
  }
}

object CliCommand {
  case class PrintAndExit(msg: String, status: ExitStatus) extends CliCommand
  case class RunScalafix(runner: CliRunner) extends CliCommand
}
