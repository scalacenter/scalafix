package scalafix.internal.sbt

import java.nio.file.Paths
import sbt.complete.Parser
import utest._

object ScalafixCompletionsTest extends TestSuite {

  val cwd = Paths.get("").toAbsolutePath
  println(cwd)
  val parser = ScalafixCompletions.parser(cwd)
  val expected = Set("-diff", "-testkit", "-sbt", "-core", "-cli")
  def check(path: String): Unit = {
    val completions = Parser.completions(parser, " file:" + path, 0)
    val obtained = completions.get.map(_.append).intersect(expected)
    assert(obtained == expected)
  }

  override def tests: Tests = Tests {
    "relpath" - check("scalafix")
    "abspath" - check(cwd.resolve("scalafix").toString)
    ".." - check(
      Paths.get("..").resolve("scalafix").resolve("scalafix").toString)
  }

}
