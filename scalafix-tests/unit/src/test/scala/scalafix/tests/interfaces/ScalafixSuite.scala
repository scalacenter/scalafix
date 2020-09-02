package scalafix.tests.interfaces

import java.net.URL
import java.nio.file.{Files, Path}
import coursierapi.Repository
import org.scalatest.funsuite.AnyFunSuite
import scalafix.interfaces.{Scalafix, ScalafixDiagnostic, ScalafixMainCallback}
import scalafix.internal.tests.utils.SkipWindows

import scala.jdk.CollectionConverters._

/**
 * Tests in this suite require scalafix-cli & its dependencies to be cross-published so that Coursier can fetch them.
 * That is done automatically as part of `sbt unit/test`, but if you run this from any other way, running
 * `sbt cli/crossPublishLocalBinTransitive` is a prerequisite.
 */
class ScalafixSuite extends AnyFunSuite {

  def tmpFile(prefix: String, suffix: String)(content: String): Path = {
    val path = Files.createTempFile(prefix, suffix)
    Files.write(path, content.getBytes)
    path
  }

  def fetchAndLoad(scalaVersion: String): Unit = {
    test(s"fetch & load instance for Scala version $scalaVersion", SkipWindows) {
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
        .withPaths(Seq(ruleSource).asJava) // any file would do, we just want rules to be loaded
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
      val args = scalafixAPI.newArguments
        .withToolClasspath(
          Seq[URL]().asJava,
          Seq[String]("com.nequissimus::sort-imports:0.5.2").asJava,
          Seq[Repository](Repository.central()).asJava
        )
      assert(args.availableRules.asScala.map(_.name).contains("RemoveUnused")) // built-in rule
      assert(args.availableRules.asScala.map(_.name).contains("SortImports")) // community rule
    }
  }
  val supportedScalaBinaryVersions = Set("2.11", "2.12", "2.13")

  // 2.11(.12) triggers `java.lang.NoClassDefFoundError: javax/tools/DiagnosticListener` on Java11.
  // See https://github.com/scala/bug/issues/10603.
  val isJava11 = Option(System.getProperty("java.version"))
    .getOrElse("")
    .startsWith("11")
  val scalaBinaryVersionsToRun =
    if (isJava11) supportedScalaBinaryVersions - "2.11"
    else supportedScalaBinaryVersions

  scalaBinaryVersionsToRun.map { scalaBinaryVersion =>
    fetchAndLoad(scalaBinaryVersion)
    fetchAndLoadWithDeps(scalaBinaryVersion)
  }
}
