package scalafix.tests.interfaces

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

import scala.jdk.CollectionConverters._

import coursierapi.Repository
import org.scalatest.funsuite.AnyFunSuite
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixMainCallback
import scalafix.internal.tests.utils.SkipWindows

/**
 * Tests in this suite require scalafix-cli & its dependencies to be
 * cross-published so that Coursier can fetch them. That is done automatically
 * as part of `sbt integrationX/test`, so make sure to run that once if you want
 * to run the test with testOnly or through BSP.
 */
class ScalafixSuite extends AnyFunSuite {

  def tmpFile(prefix: String, suffix: String)(content: String): Path = {
    val path = Files.createTempFile(prefix, suffix)
    Files.write(path, content.getBytes)
    path
  }

  def fetchAndLoad(scalaVersion: String): Unit = {
    test(
      s"fetch & load instance for Scala version $scalaVersion",
      SkipWindows
    ) {
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
        .withPaths(
          Seq(ruleSource).asJava
        ) // any file would do, we just want rules to be loaded
        .withRules(Seq(s"file:$ruleSource").asJava)
        .withMainCallback(scalafixMainCallback)
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
  val supportedScalaBinaryVersions: Set[String] = Set("2.12", "2.13")

  supportedScalaBinaryVersions.map { scalaBinaryVersion =>
    fetchAndLoad(scalaBinaryVersion)
    fetchAndLoadWithDeps(scalaBinaryVersion)
  }
}
