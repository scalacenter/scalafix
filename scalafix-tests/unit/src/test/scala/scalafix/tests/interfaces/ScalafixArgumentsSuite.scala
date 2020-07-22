package scalafix.tests.interfaces

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.scalatest.funsuite.AnyFunSuite
import scalafix.interfaces.{Scalafix, ScalafixArguments, ScalafixMainMode}
import scalafix.internal.reflect.{ClasspathOps, RuleCompiler}
import scalafix.internal.rule.{RemoveUnused, RemoveUnusedConfig}
import scalafix.test.StringFS
import scalafix.v1.SemanticRule
import scala.meta.io.AbsolutePath
import collection.JavaConverters._

/**
 * Tests in this suite require scalafix-cli & its dependencies to be cross-published so that Coursier can fetch them.
 * That is done automatically as part of `sbt unit/test`, but if you run this from any other way, running
 * `sbt cli/crossPublishLocalBinTransitive` is a prerequisite.
 */
class ScalafixArgumentsSuite extends AnyFunSuite {
  val scalaBinaryVersion = "2.12"
  val scalaVersion = "2.12.11"
  val api: ScalafixArguments = Scalafix
    .fetchAndClassloadInstance(scalaBinaryVersion)
    .newArguments()

  val charset = StandardCharsets.US_ASCII
  val cwd = StringFS
    .string2dir(
      """|/src/Main.scala
         |import scala.concurrent.duration
         |import scala.concurrent.Future
         |
         |object Main extends App {
         |  import scala.concurrent.Await
         |  println("test")
         |}
      """.stripMargin,
      charset
    )
  val d = cwd.resolve("out").toNIO
  val src = cwd.resolve("src").toNIO
  Files.createDirectories(d)
  val main = src.resolve("Main.scala")

  val scalacOptions = Array[String](
    "-Yrangepos",
    s"-Xplugin:${semanticdbPluginPath()}",
    "-Xplugin-require:semanticdb",
    "-classpath",
    scalaLibrary.toString,
    s"-P:semanticdb:sourceroot:$src",
    "-d",
    d.toString,
    main.toString,
  )

  test("ScalafixArgumentsSuite test RemoveUnused rule") {
    val scalacOptions = List("-Wunused:imports")
    val result = api
      .withRules(
        List(removeUnsuedRule().name.toString(), "ExplicitResultTypes").asJava
      )
      .withClasspath(Seq(scalaLibrary.toNIO).asJava)
      .withScalaVersion(scalaVersion)
      .withScalacOptions(scalacOptions.asJava)
      .withPaths(Seq(main).asJava)
      .withSourceroot(src)
      .runAndReturnResult()

    println(s"get error ${result.getError}")
    println(result.getScalafixOutputs.head.getOutputFileFixed)
    println(result.getScalafixOutputs.head.getUnifiedDiff)

  }
  def removeUnsuedRule(): SemanticRule = {
    val config = RemoveUnusedConfig.default
    new RemoveUnused(config)
  }
  def scalaLibrary: AbsolutePath =
    RuleCompiler.defaultClasspathPaths
      .find(_.toNIO.getFileName.toString.contains("scala-library"))
      .getOrElse {
        throw new IllegalStateException("Unable to detect scala-library.jar")
      }

  def semanticdbPluginPath(): String = {
    val semanticdbscalac = ClasspathOps.thisClassLoader.getURLs.collectFirst {
      case url if url.toString.contains("semanticdb-scalac_") =>
        Paths.get(url.toURI).toString
    }
    semanticdbscalac.getOrElse {
      throw new IllegalStateException(
        "unable to auto-detect semanticdb-scalac compiler plugin"
      )
    }
  }
}
