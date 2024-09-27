package scalafix.tests.rules

import scala.meta.io.AbsolutePath

import metaconfig.Configured
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.rule.ExplicitResultTypes
import scalafix.v1.Configuration

class ExplicitResultTypesSuite extends AnyFunSuite {
  val rule = new ExplicitResultTypes()
  val config: Configuration =
    Configuration().withScalacClasspath(List(AbsolutePath.root))

  test("Scala 3 is not supported") {
    val scala3 =
      config.withScalaVersion("3.3.4")

    val expected =
      "The ExplicitResultTypes rule needs to run with the same Scala binary version as the one used to compile target sources (3.3). " +
        "To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or make sure Scalafix is loaded with 3.3."

    assert(rule.withConfiguration(scala3) == Configured.error(expected))
  }

}
