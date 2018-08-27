package scalafix.tests.cli

import com.geirsson.coursiersmall.CoursierSmall
import com.geirsson.coursiersmall.Dependency
import com.geirsson.coursiersmall.Settings
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import org.scalatest.FunSuite
import scala.collection.JavaConverters._
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scalafix.Versions
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixException
import scalafix.interfaces.ScalafixMainCallback
import scalafix.interfaces.ScalafixMainMode
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.{interfaces => i}

class ScalafixImplSuite extends FunSuite with DiffAssertions {
  def semanticdbPluginPath(): String = {
    val semanticdbscalac = ClasspathOps.thisClassLoader.getURLs.collectFirst {
      case url if url.toString.contains("semanticdb-scalac_") =>
        Paths.get(url.toURI).toString
    }
    semanticdbscalac.getOrElse {
      throw new IllegalStateException(
        "unable to auto-detect semanticdb-scalac compiler plugin")
    }
  }
  def scalaLibrary: AbsolutePath =
    RuleCompiler.defaultClasspathPaths
      .find(_.toNIO.getFileName.toString.contains("scala-library"))
      .getOrElse {
        throw new IllegalStateException("Unable to detect scala-library.jar")
      }

  test("versions") {
    val api = i.Scalafix.classloadInstance(this.getClass.getClassLoader)
    assert(api.scalafixVersion() == Versions.version)
    assert(api.scalametaVersion() == Versions.scalameta)
    assert(api.scala211() == Versions.scala211)
    assert(api.scala212() == Versions.scala212)
    assert(
      api
        .supportedScalaVersions()
        .sameElements(Versions.supportedScalaVersions)
    )
    val help = api.mainHelp(80)
    assert(help.contains("Usage: scalafix"))
  }

  test("availableRules") {
    val api = i.Scalafix.classloadInstance(this.getClass.getClassLoader)
    val rules = api.newMainArgs().availableRules().asScala.map(_.name())
    assert(rules.contains("DisableSyntax"))
    assert(rules.contains("AvailableRule"))
    assert(!rules.contains("DeprecatedAvailableRule"))
  }

  test("error") {
    val cl = new URLClassLoader(Array())
    val ex = intercept[ScalafixException] {
      i.Scalafix.classloadInstance(cl)
    }
    assert(ex.getCause.isInstanceOf[ClassNotFoundException])
  }

  test("runMain") {
    // This is a full integration test that stresses the full breadth of the scalafix-interfaces API
    val api = i.Scalafix.classloadInstance(this.getClass.getClassLoader)
    // Assert that non-ascii characters read into "?"
    val charset = StandardCharsets.US_ASCII
    val cwd = StringFS
      .string2dir(
        """|/src/Semicolon.scala
           |
           |object Semicolon {
           |  val a = 1; // みりん þæö
           |  implicit val b = List(1)
           |  def main { println(42) }
           |}
           |
           |/src/Excluded.scala
           |object Excluded {
           |  val a = 1;
           |}
      """.stripMargin,
        charset
      )
      .toNIO
    val d = cwd.resolve("out")
    val src = cwd.resolve("src")
    Files.createDirectories(d)
    val semicolon = src.resolve("Semicolon.scala")
    val excluded = src.resolve("Excluded.scala")
    val dependency =
      new Dependency("com.geirsson", "example-scalafix-rule_2.12", "1.1.0")
    val settings = new Settings().withDependencies(List(dependency))
    // This rule is published to Maven Central to simplify testing --tool-classpath.
    val toolClasspathJars = CoursierSmall.fetch(settings)
    val toolClasspath = ClasspathOps.toClassLoader(
      Classpath(toolClasspathJars.map(jar => AbsolutePath(jar))))
    val scalacOptions = Array[String](
      "-Yrangepos",
      s"-Xplugin:${semanticdbPluginPath()}",
      "-Xplugin-require:semanticdb",
      "-classpath",
      scalaLibrary.toString,
      s"-P:semanticdb:sourceroot:$src",
      "-d",
      d.toString,
      semicolon.toString,
      excluded.toString
    )
    val compileSucceeded = scala.tools.nsc.Main.process(scalacOptions)
    assert(compileSucceeded)
    val buf = List.newBuilder[ScalafixDiagnostic]
    val callback = new ScalafixMainCallback {
      override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit = {
        buf += diagnostic
      }
    }
    val out = new ByteArrayOutputStream()
    val relativePath = cwd.relativize(semicolon)
    val args = api
      .newMainArgs()
      .withArgs(List("--settings.DisableSyntax.noSemicolons", "true").asJava)
      .withCharset(charset)
      .withClasspath(List(d, scalaLibrary.toNIO).asJava)
      .withSourceroot(src)
      .withWorkingDirectory(cwd)
      .withPaths(List(relativePath.getParent).asJava)
      .withExcludedPaths(
        List(
          FileSystems.getDefault.getPathMatcher("glob:**Excluded.scala")
        ).asJava
      )
      .withMainCallback(callback)
      .withRules(
        List(
          "DisableSyntax", // syntactic linter
          "ProcedureSyntax", // syntactic rewrite
          "ExplicitResultTypes", // semantic rewrite
          "class:fix.Examplescalafixrule_v1" // --tool-classpath
        ).asJava
      )
      .withPrintStream(new PrintStream(out))
      .withMode(ScalafixMainMode.TEST)
      .withToolClasspath(toolClasspath)
      .withScalacOptions(Collections.singletonList("-Ywarn-unused-import"))
      .withScalaVersion("2.11.12")
    val errors = api.runMain(args).toList.map(_.toString).sorted
    val stdout = fansi
      .Str(out.toString(charset.name()))
      .plainText
      .replaceAllLiterally(semicolon.toString, relativePath.toString)
      .replace('\\', '/') // for windows
      .lines
      .filterNot(_.trim.isEmpty)
      .mkString("\n")
    assert(errors == List("LinterError", "TestError"), stdout)
    val linterDiagnostics = buf
      .result()
      .map { d =>
        d.position()
          .get()
          .formatMessage(d.severity().toString, d.message())
      }
      .mkString("\n\n")
      .replaceAllLiterally(semicolon.toString, relativePath.toString)
      .replace('\\', '/') // for windows
    assertNoDiff(
      linterDiagnostics,
      """|src/Semicolon.scala:3:12: ERROR: semicolons are disabled
         |  val a = 1; // ??? ???
         |           ^
      """.stripMargin
    )
    assertNoDiff(
      stdout,
      """|--- src/Semicolon.scala
         |+++ <expected fix>
         |@@ -1,6 +1,7 @@
         | object Semicolon {
         |   val a = 1; // ??? ???
         |-  implicit val b = List(1)
         |-  def main { println(42) }
         |+  implicit val b: _root_.scala.collection.immutable.List[_root_.scala.Int] = List(1)
         |+  def main: Unit = { println(42) }
         | }
         |+// Hello world!
         |""".stripMargin
    )
  }

}
