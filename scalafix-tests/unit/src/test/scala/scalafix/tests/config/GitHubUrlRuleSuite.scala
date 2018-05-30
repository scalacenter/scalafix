package scalafix.tests.config

import metaconfig.Conf
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import org.scalatest.FunSuite
import scalafix.internal.reflect.GitHubUrlRule

class GitHubUrlRuleSuite extends FunSuite {
  def check(original: String, expected: String, ok: Boolean = true): Unit = {
    test((if (ok) "" else "FAIL ") + original) {
      Conf.Str(original) match {
        case GitHubUrlRule(Ok(obtained)) if ok =>
          assert(obtained.toString == expected)
        case GitHubUrlRule(NotOk(obtained)) if !ok =>
          assert(obtained.toString == expected)
      }
    }
  }
  def checkFail(original: String, expected: String): Unit = {
    check(original, expected, ok = false)
  }

  check(
    "github:someorg/somerepo/1.2.3",
    "https://raw.githubusercontent.com/someorg/somerepo/master/scalafix/rules/" +
      "src/main/scala/fix/Somerepo_1_2_3.scala"
  )
  check(
    "github:someorg/somerepo/1.2.3?sha=master~1",
    "https://raw.githubusercontent.com/someorg/somerepo/master~1/scalafix/rules/" +
      "src/main/scala/fix/Somerepo_1_2_3.scala"
  )
  check(
    "github:someorg/some-repo/1.2.3",
    "https://raw.githubusercontent.com/someorg/some-repo/master/scalafix/rules/" +
      "src/main/scala/fix/Somerepo_1_2_3.scala"
  )
  check(
    "github:someorg/42some-repo/1.2.3",
    "https://raw.githubusercontent.com/someorg/42some-repo/master/scalafix/rules/" +
      // NOTE: identifiers can't start with numbers like 42. However,
      // giter8 doesn't support adding a prefix in case the first character
      // is a number: http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
      // The rule inside the file can still be renamed to _42SomeRepo
      // without problem.
      "src/main/scala/fix/42somerepo_1_2_3.scala"
  )
  checkFail(
    "github:someorg/somerepo",
    """Invalid url 'github:someorg/somerepo'. Valid formats are:
      |- github:org/repo/version
      |- github:org/repo/version?sha=branch""".stripMargin
  )
}
