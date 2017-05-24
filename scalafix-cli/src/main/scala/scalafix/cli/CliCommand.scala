package scalafix.cli

sealed abstract class CliCommand {
  import CliCommand._
  def isOk: Boolean = !isError
  def isError: Boolean = this match {
    case RunScalafix(_) => false
    case _ => true
  }
}

object CliCommand {
  case class PrintAndExit(msg: String, status: ExitStatus) extends CliCommand
  case class RunScalafix(runner: CliRunner) extends CliCommand
}