package scalafix.tests

import java.io.File

import ammonite.ops.Path
import ammonite.ops._

case class ItTest(
    name: String,
    repo: String,
    hash: String,
    config: String = "",
    commands: Seq[Command] = Command.default,
    rules: Seq[String] = Nil,
    addCoursier: Boolean = true
) {
  def repoName: String = repo match {
    case Command.RepoName(x) => x
    case _ =>
      throw new IllegalArgumentException(
        s"Unable to parse repo name from repo: $repo")
  }
  def workingPath: Path = ItTest.root / repoName
  def parentDir: File = workingPath.toIO.getParentFile
}

object ItTest {
  val root: Path = pwd / "target" / "it"
}
