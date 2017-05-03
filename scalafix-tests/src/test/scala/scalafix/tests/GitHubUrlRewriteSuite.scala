package scalafix
package tests

import scalafix.reflect.ScalafixCompilerDecoder.GitHubUrlRewrite

import org.scalatest.{FunSuiteLike, Matchers}
import metaconfig.Conf

class GitHubUrlRewriteSuite extends FunSuiteLike with Matchers {
  test("it expands a GitHub shorthand with a default sha") {
    Conf.Str("github:someorg/somerepo/1.2.3") match {
      case GitHubUrlRewrite(url) =>
        url.toString shouldBe "https://github.com/someorg/somerepo/blob/master/scalafix-rewrites/src/main/scala/somerepo/scalafix/Somerepo_1_2_3.scala"
    }
  }

  test("it expands a GitHub shorthand with a specific sha") {
    Conf.Str("github:someorg/somerepo/1.2.3?sha=master~1") match {
      case GitHubUrlRewrite(url) =>
        url.toString shouldBe "https://github.com/someorg/somerepo/blob/master~1/scalafix-rewrites/src/main/scala/somerepo/scalafix/Somerepo_1_2_3.scala"
    }
  }

  test("it replaces invalid characters in package name and file name with _") {
    Conf.Str("github:someorg/some-repo/1.2.3") match {
      case GitHubUrlRewrite(url) =>
        url.toString shouldBe "https://github.com/someorg/some-repo/blob/master/scalafix-rewrites/src/main/scala/some_repo/scalafix/Some_repo_1_2_3.scala"
    }
  }

  test(
    "it adds an underscore to the package name and to the file name if the repo name begins with a digit") {
    Conf.Str("github:someorg/42some-repo/1.2.3") match {
      case GitHubUrlRewrite(url) =>
        url.toString shouldBe "https://github.com/someorg/42some-repo/blob/master/scalafix-rewrites/src/main/scala/_42some_repo/scalafix/_42some_repo_1_2_3.scala"
    }
  }
}
