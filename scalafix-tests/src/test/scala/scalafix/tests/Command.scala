package scalafix.tests

import scala.util.matching.Regex

case class Command(cmd: String, optional: Boolean = false)

object Command {
  val testCompile =
    Command("test:compile", optional = true)
  val scalafixTask =
    Command("scalafix", optional = true)
  def default: Seq[Command] = Seq(
    scalafixTask,
    testCompile
  )
  val RepoName: Regex = ".*/([^/].*).git".r
}