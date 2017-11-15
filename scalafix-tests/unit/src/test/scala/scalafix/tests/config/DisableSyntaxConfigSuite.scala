package scalafix.tests.config

import org.scalatest.FunSuite

import scala.meta.inputs.Input

import metaconfig.Configured.NotOk
import metaconfig.ConfError

import scalafix.internal.config.MetaconfigParser
import scalafix.internal.config.DisableSyntaxConfig

class DisableSyntaxConfigSuite extends FunSuite {
  test("invalid keywords") {
    val input = Input.String(
      """|keywords = [
         |  banana
         |]
         |""".stripMargin
    )
    val obtained = 
      MetaconfigParser.parser.fromInput(input).andThen(conf =>
        DisableSyntaxConfig.reader.read(conf)
      )

    val error =
      """|Type mismatch;
         |  found    : String (value: "banana")
         |  expected : String, one of: bar | foo""".stripMargin

    val expected = NotOk(ConfError.msg(error))

    assert(obtained == expected)
  }
}