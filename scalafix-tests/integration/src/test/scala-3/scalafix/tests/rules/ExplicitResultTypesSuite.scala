package scalafix.tests.rules

import scala.meta.io.AbsolutePath

import metaconfig.Conf
import metaconfig.Configured
import metaconfig.typesafeconfig.*
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

  test("Other Scala 3 minor versions are not supported by default") {
    val neitherLTSNorNext =
      config.withScalaVersion("3.4.0")

    val v =
      buildinfo.RulesBuildInfo.scalaVersion.split('.').take(2).mkString(".")

    val expected =
      s"The ExplicitResultTypes rule was compiled with a different Scala 3 minor ($v) than the target sources (3.4). " +
        "To fix this problem, make sure you are running the latest version of Scalafix. " +
        "If that is the case, either change your build to stick to the Scala 3 LTS or Next versions supported by Scalafix, or " +
        "enable ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch in .scalafix.conf in order to try to load what is needed dynamically."

    assert(
      rule.withConfiguration(neitherLTSNorNext) == Configured.error(expected)
    )
  }

  test(
    "fetchScala3CompilerArtifactsOnVersionMismatch unlocks dynamic loading"
  ) {
    val neitherLTSNorNext =
      config.withScalaVersion("3.5.0")

    val conf = Conf
      .parseString(
        "ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch = true"
      )
      .get
    assert(rule.withConfiguration(neitherLTSNorNext.withConf(conf)).isOk)
  }

}
