package scalafix.internal.sbt

import java.nio.file.Paths
import org.scalatest.FunSuite
import sbt.complete.Parser

class ScalafixCompletionsTest extends FunSuite {
  val cwd = Paths.get("").toAbsolutePath
  println(cwd)
  val parser = ScalafixCompletions.parser(cwd)
  val expected = Set("-diff", "-testkit", "-sbt", "-core", "-cli")
  def check(path: String): Unit = {
    test(path) {
      val completions = Parser.completions(parser, " file:" + path, 0)
      val obtained = completions.get.map(_.append).intersect(expected)
      assert(obtained == expected)
    }
  }

  check("scalafix") // relative path
  check(cwd.resolve("scalafix").toString) // absolute path
  check(Paths.get("..").resolve("scalafix").resolve("scalafix").toString)
}
