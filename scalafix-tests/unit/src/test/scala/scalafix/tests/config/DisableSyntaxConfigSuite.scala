package scalafix.tests.config

import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.typesafeconfig._
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.rule._

class DisableSyntaxConfigSuite extends AnyFunSuite {
  test("Warn about invalid keywords") {
    val rawConfig =
      """|keywords = [
        |  banana
        |]
        |""".stripMargin
    val errorMessage = "banana is not in our supported keywords."
    assertError(rawConfig, errorMessage)
  }

  test("Provide suggestions when typos are present in keywords") {
    val rawConfig =
      """|keywords = [
        |  overide
        |]
        |""".stripMargin
    val errorMessage =
      "overide is not in our supported keywords. (Did you mean: override?)"
    assertError(rawConfig, errorMessage)
  }

  test("Warn about wrong types") {
    val rawConfig =
      """|keywords = [
        |  42
        |]
        |""".stripMargin
    val errorMessage =
      """|<input>:2:0 error: Type mismatch;
        |  found    : Number (value: 42)
        |  expected : String
        |  42
        |^
        |""".stripMargin
    assertError(rawConfig, errorMessage)
  }

  test("Handles non-string types") {
    val rawConfig =
      """|keywords = [
        |  null
        |  false
        |  true
        |]
        |""".stripMargin

    val obtained = read(rawConfig).get
    val expected = DisableSyntaxConfig(
      keywords = Set(
        DisabledKeyword("null"),
        DisabledKeyword("false"),
        DisabledKeyword("true")
      )
    )

    assert(obtained == expected)
  }

  def read(rawConfig: String): Configured[DisableSyntaxConfig] = {
    val input = metaconfig.Input.String(rawConfig)
    metaconfig.Conf
      .parseInput(input)
      .andThen(conf => DisableSyntaxConfig.decoder.read(conf))
  }

  def assertError(rawConfig: String, errorMessage: String): Unit = {
    val obtained = read(rawConfig)
    assert(obtained.asInstanceOf[NotOk].error.msg == errorMessage)
  }
}
