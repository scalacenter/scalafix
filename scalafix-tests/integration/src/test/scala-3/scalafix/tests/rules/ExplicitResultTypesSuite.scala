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

  test("Scala 2 is not supported") {
    val scala2 =
      config.withScalaVersion("2.12.15")

    val expected =
      "The ExplicitResultTypes rule needs to run with the same Scala binary version as the one used to compile target sources (2.12). " +
        "To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or make sure Scalafix is loaded with 2.12."

    assert(rule.withConfiguration(scala2) == Configured.error(expected))
  }

  test("Early Scala 3 is not supported") {
    val earlyScala3 =
      config.withScalaVersion("3.2.2")

    val expected =
      "The ExplicitResultTypes rule requires Scala 3 target sources to be compiled with Scala 3.3.0 or greater, but they were compiled with 3.2.2. " +
        "To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or upgrade the compiler in your build."

    assert(rule.withConfiguration(earlyScala3) == Configured.error(expected))
  }

}
