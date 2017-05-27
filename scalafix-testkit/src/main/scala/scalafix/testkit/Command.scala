package scalafix.tests

import scala.util.matching.Regex

case class Command(cmd: String) {
  override def toString: String = cmd
}

object Command {
  val enableWarnUnusedImports =
    Command("set scalacOptions in ThisBuild += \"-Ywarn-unused-import\" ")
  val clean =
    Command("clean")
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
