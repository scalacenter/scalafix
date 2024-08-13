package scalafix.tests.interfaces

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

import scala.jdk.CollectionConverters._

import coursierapi.Repository
import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixException
import scalafix.interfaces.ScalafixMainCallback

class ScalafixSuite extends AnyFunSuite {

  test("versions") {
    val api = Scalafix.classloadInstance(this.getClass.getClassLoader)
    assert(api.scalafixVersion() == Versions.version)
    assert(api.scalametaVersion() == Versions.scalameta)
    assert(api.scala212() == Versions.scala212)
    assert(api.scala213() == Versions.scala213)
    assert(
      api
        .supportedScalaVersions()
        .sameElements(Versions.supportedScalaVersions)
    )
    val help = api.mainHelp(80)
    assert(help.contains("Usage: scalafix"))
  }

  test("error") {
    val cl = new URLClassLoader(Array(), null)
    val ex = intercept[ScalafixException] {
      Scalafix.classloadInstance(cl)
    }
    assert(ex.getCause.isInstanceOf[ClassNotFoundException])
  }

  def tmpFile(prefix: String, suffix: String)(content: String): Path = {
    val path = Files.createTempFile(prefix, suffix)
    Files.write(path, content.getBytes)
    path
  }

  def fetchAndLoad(scalaVersion: String): Unit = {
    test(s"fetch & load instance for Scala version $scalaVersion") {
      val scalafixAPI = Scalafix.fetchAndClassloadInstance(
        scalaVersion,
        Seq[Repository](
          Repository.ivy2Local(), // for scalafix-*
          Repository.central() // for scala libs
        ).asJava
      )
      val args = scalafixAPI.newArguments

      assert(args.availableRules.asScala.map(_.name).contains("RemoveUnused"))

      // inspect the tool classpath Scala version by running a custom rule inside it
      val ruleSource =
        tmpFile("CaptureScalaVersion", ".scala") {
          """import scalafix.v1._
            |class CaptureScalaVersion extends SyntacticRule("CaptureScalaVersion") {
            |  override def fix(implicit doc: SyntacticDocument): Patch =
            |    Patch.lint(
            |      Diagnostic(
            |        "",
            |        util.Properties.versionNumberString,
            |        scala.meta.Position.None,
            |        "",
            |        scalafix.lint.LintSeverity.Error
            |      )
            |    )
            |}""".stripMargin
        }
      var maybeDiagnostic: Option[ScalafixDiagnostic] = None
      val scalafixMainCallback = new ScalafixMainCallback {
        override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit =
          maybeDiagnostic = Some(diagnostic)
      }
      args
        .withRules(Seq(ruleSource.toUri.toString).asJava)
        .withMainCallback(scalafixMainCallback)
        // any target file would do to emit a diagnostic, so run the rule on itself
        .withPaths(List(ruleSource).asJava)
        .withWorkingDirectory(ruleSource.getParent)
        .run()
      assert(maybeDiagnostic.get.message.startsWith(scalaVersion))
    }
  }

  def fetchAndLoadWithDeps(scalaVersion: String): Unit = {
    test(
      s"fetch & load instance for Scala version $scalaVersion with external dependencies"
    ) {
      val scalafixAPI = Scalafix.fetchAndClassloadInstance(scalaVersion)

      val ruleForDependency = Map(
        // built against scalafix 0.9.16
        "com.nequissimus::sort-imports:0.5.2" -> "SortImports",
        // built against scalafix 0.10.0, uses metaconfig
        "com.github.xuwei-k::scalafix-rules:0.2.1" -> "KindProjector"
      )

      val availableRules = scalafixAPI.newArguments
        .withToolClasspath(
          Seq[URL]().asJava,
          ruleForDependency.keys.toList.asJava,
          Seq[Repository](Repository.central()).asJava
        )
        .availableRules
        .asScala
        .map(_.name)

      assert(availableRules.contains("RemoveUnused")) // built-in
      ruleForDependency.values.foreach { rule =>
        assert(availableRules.contains(rule))
      }
    }
  }

  /**
   * Tests below require scalafix-cli & its dependencies to be cross-published
   * so that Coursier can fetch them. That is done automatically as part of `sbt
   * integrationX / test`, so make sure to run that once if you want to run the
   * test with testOnly or through BSP.
   */
  val supportedScalaBinaryVersions: Set[String] = Set("2.12", "2.13")
  supportedScalaBinaryVersions.map { scalaBinaryVersion =>
    fetchAndLoad(scalaBinaryVersion)
    fetchAndLoadWithDeps(scalaBinaryVersion)
  }
}
