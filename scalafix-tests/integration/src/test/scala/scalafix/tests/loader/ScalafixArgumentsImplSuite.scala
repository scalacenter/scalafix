package scalafix.tests.loader

import java.util.Optional

import scala.collection.JavaConverters._

import scala.meta.io.AbsolutePath
import scala.meta.testkit.StringFS

import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.interfaces.ScalafixArguments
import scalafix.loader.internal.ScalafixArgumentsImpl
import scalafix.tests.BuildInfo

/**
 * Some tests below require scalafix-cli & its dependencies to be published so
 * that Coursier can fetch them. `publishLocalTransitive` is done automatically
 * as part of `sbt integrationX / test`, so make sure to run that once if you
 * want to run the test with testOnly or through BSP.
 */
class ScalafixArgumentsImplSuite extends AnyFunSuite {

  val scalaVersion: String = BuildInfo.scalaVersion

  val workingDirectory: AbsolutePath = StringFS.fromString(
    s"""
      |/.scalafix.conf
      |version = "${Versions.version}"
      |rules = ExplicitResultTypes
      |ExplicitResultTypes.skipSimpleDefinitions = []
      |
      |/src/Main.scala
      |object Main {
      |  def foo = 2
      |}
      |""".stripMargin
  )

  val arguments: ScalafixArguments =
    new ScalafixArgumentsImpl()
      .withWorkingDirectory(workingDirectory.toNIO)
      .withScalaVersion(scalaVersion)

  test("availableRules") {
    val availableRules = arguments
      .availableRules()
      .asScala
    assert(availableRules.exists(_.name() == "ExplicitResultTypes"))
  }

  test("rulesThatWillRun") {
    val rulesThatWillRun = arguments
      .rulesThatWillRun()
      .asScala
    assert(rulesThatWillRun.map(_.name()) == Seq("ExplicitResultTypes"))
  }

  test("validate") {
    assert(arguments.validate() == Optional.empty())
  }

  test("evaluate") {}

  test("run --check") {}
}
