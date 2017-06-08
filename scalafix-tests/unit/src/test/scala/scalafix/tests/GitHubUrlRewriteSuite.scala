package scalafix
package tests

import scalafix.reflect.ScalafixCompilerDecoder.GitHubUrlRewrite
import metaconfig.Conf
import org.scalatest.FunSuite

class GithubUrlRewriteSuite extends FunSuite {
  def check(original: String, expected: String): Unit = {
    test(original) {
      Conf.Str(original) match {
        case GitHubUrlRewrite(obtained) =>
          assert(obtained.toString == expected)
      }
    }
  }

  check(
    "github:someorg/somerepo/1.2.3",
    "https://github.com/someorg/somerepo/blob/master/scalafix/rewrites/" +
      "src/main/scala/fix/Somerepo_1_2_3.scala"
  )
  check(
    "github:someorg/somerepo/1.2.3?sha=master~1",
    "https://github.com/someorg/somerepo/blob/master~1/scalafix/rewrites/" +
      "src/main/scala/fix/Somerepo_1_2_3.scala"
  )
  check(
    "github:someorg/some-repo/1.2.3",
    "https://github.com/someorg/some-repo/blob/master/scalafix/rewrites/" +
      "src/main/scala/fix/SomeRepo_1_2_3.scala"
  )
  check(
    "github:someorg/42some-repo/1.2.3",
    "https://github.com/someorg/42some-repo/blob/master/scalafix/rewrites/" +
      // NOTE: identifiers can't start with numbers like 42. However,
      // giter8 doesn't support adding a prefix in case the first character
      // is a number: http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
      // The rewrite inside the file can still be renamed to _42SomeRepo
      // without problem.
      "src/main/scala/fix/42someRepo_1_2_3.scala"
  )
}
