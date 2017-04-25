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
}
