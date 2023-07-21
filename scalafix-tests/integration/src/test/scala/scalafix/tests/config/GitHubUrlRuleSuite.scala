package scalafix.tests.config

import metaconfig.Conf
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.reflect.GitHubUrlRule
import scalafix.testkit.DiffAssertions

class GitHubUrlRuleSuite extends AnyFunSuite with DiffAssertions {
  def check(original: String, expected: String, ok: Boolean = true): Unit = {
    test((if (ok) "" else "FAIL ") + original) {
      Conf.Str(original) match {
        case GitHubUrlRule(Ok(obtained)) if ok =>
          assertNoDiffOrPrintExpected(obtained.toString, expected)
        case GitHubUrlRule(NotOk(obtained)) if !ok =>
          assertNoDiffOrPrintExpected(obtained.toString, expected)
      }
    }
  }
  def checkFail(original: String, expected: String): Unit = {
    check(original, expected, ok = false)
  }

  check(
    "github:someorg/some-repo",
    "https://raw.githubusercontent.com/someorg/some-repo/master/scalafix/rules/" +
      "src/main/scala/fix/SomeRepo.scala"
  )
  check(
    "github:typelevel/cats/v1_0",
    "https://raw.githubusercontent.com/typelevel/cats/master/scalafix/rules/" +
      "src/main/scala/fix/v1_0.scala"
  )
  check(
    "github:someorg/some-repo/RuleName",
    "https://raw.githubusercontent.com/someorg/some-repo/master/scalafix/rules/" +
      "src/main/scala/fix/RuleName.scala"
  )
  check(
    "github:someorg/some-repo/com.example.RuleName",
    "https://raw.githubusercontent.com/someorg/some-repo/master/scalafix/rules/" +
      "src/main/scala/com/example/RuleName.scala"
  )
  check(
    "github:someorg/some-repo/RuleName?sha=master~1",
    "https://raw.githubusercontent.com/someorg/some-repo/master~1/scalafix/rules/" +
      "src/main/scala/fix/RuleName.scala"
  )
  check(
    "github:someorg/some-repo/com.example.RuleName?sha=1234abc",
    "https://raw.githubusercontent.com/someorg/some-repo/1234abc/scalafix/rules/" +
      "src/main/scala/com/example/RuleName.scala"
  )
  check(
    "github:someorg/42some-repo/RuleName",
    "https://raw.githubusercontent.com/someorg/42some-repo/master/scalafix/rules/" +
      // NOTE: identifiers can't start with numbers like 42. However,
      // giter8 doesn't support adding a prefix in case the first character
      // is a number: http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
      // The rule inside the file can still be renamed to _42SomeRepo
      // without problem.
      "src/main/scala/fix/RuleName.scala"
  )
  checkFail(
    "github:someorgsomerepo",
    """Invalid url 'github:someorgsomerepo'. Valid formats are:
      |- github:org/repo
      |- github:org/repo/name
      |- github:org/repo/name?sha=branch""".stripMargin
  )
}
