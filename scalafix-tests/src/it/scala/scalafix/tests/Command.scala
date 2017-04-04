package scalafix.tests

import scala.util.matching.Regex

case class Command(cmd: String) {
  override def toString: String = cmd
}

object Command {
  val clean =
    Command("clean")
  val enableScalafix =
    Command("set scalafixEnabled in Global := true")
  val disableScalafix =
    Command("set scalafixEnabled in Global := false")
  val testCompile =
    Command("test:compile")
  val scalafixTask =
    Command("scalafix")
  def default: Seq[Command] = Seq(
    scalafixTask,
    testCompile
  )
  val RepoName: Regex = ".*/([^/].*).git".r
}
