package scalafix.tests.interfaces

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections

import scala.collection.JavaConverters._

import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

import buildinfo.RulesBuildInfo
import org.scalatest.funsuite.AnyFunSuite
import scalafix.interfaces.ScalafixArguments
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixDialect
import scalafix.interfaces.ScalafixFileEvaluationError
import scalafix.interfaces.ScalafixMainCallback
import scalafix.interfaces.ScalafixMainMode
import scalafix.internal.interfaces.ScalafixArgumentsImpl
import scalafix.internal.rule.RemoveUnused
import scalafix.internal.rule.RemoveUnusedConfig
import scalafix.internal.tests.utils.SkipWindows
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.tests.core.Classpaths
import scalafix.tests.util.ScalaVersions
import scalafix.tests.util.SemanticdbPlugin
import scalafix.v1.SemanticRule

class ScalafixArgumentsSuite extends AnyFunSuite with DiffAssertions {
  val scalaBinaryVersion: String =
    RulesBuildInfo.scalaVersion.split('.').take(2).mkString(".")
  val scalaVersion = RulesBuildInfo.scalaVersion
  val removeUnused: String =
    if (ScalaVersions.isScala213)
      "-Wunused:imports"
    else "-Ywarn-unused-import"
  val api: ScalafixArguments =
    ScalafixArgumentsImpl()
      .withScalaVersion(scalaVersion)

  val charset = StandardCharsets.US_ASCII
  val cwd: Path = StringFS
    .string2dir(
      """|/src/Main.scala
        |import scala.concurrent.duration
        |import scala.concurrent.Future
        |
        |object Main extends App {
        |  import scala.concurrent.Await
        |  println("test");
        |  println("ok")
        |}
      """.stripMargin,
      charset
    )
    .toNIO
  val d: Path = cwd.resolve("out")
  val target: Path = cwd.resolve("target")
  val src: Path = cwd.resolve("src")
  Files.createDirectories(d)
  val main: Path = src.resolve("Main.scala")
  val relativePath: Path = cwd.relativize(main)

  val scalacOptions: Array[String] = Array[String](
    "-Yrangepos",
    removeUnused,
    s"-Xplugin:${SemanticdbPlugin.semanticdbPluginPath()}",
    "-Xplugin-require:semanticdb",
    "-classpath",
    s"${scalaLibrary.mkString(":")}",
    s"-P:semanticdb:sourceroot:$src",
    s"-P:semanticdb:targetroot:$target",
    "-d",
    d.toString,
    main.toString
  )

  test("ScalafixArguments.evaluate with a semantic rule", SkipWindows) {
    val _ = scala.tools.nsc.Main.process(scalacOptions)
    val result = api
      .withRules(
        List(
          removeUnsuedRule().name.toString(),
          "ExplicitResultTypes",
          "DisableSyntax"
        ).asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .withClasspath((scalaLibrary.map(_.toNIO) :+ target).asJava)
      .withScalacOptions(Collections.singletonList(removeUnused))
      .withPaths(Seq(main).asJava)
      .withSourceroot(src)
      .evaluate()

    val error = result.getError
    assert(!error.isPresent) // we ignore completely linterErrors
    assert(result.isSuccessful)
    assert(result.getFileEvaluations.length == 1)
    val fileEvaluation = result.getFileEvaluations.head
    assert(fileEvaluation.isSuccessful)
    val expected =
      """|
        |object Main extends App {
        |  println("test");
        |  println("ok")
        |}
        |""".stripMargin
    val obtained = fileEvaluation.previewPatches.get()
    assertNoDiff(obtained, expected)

    val linterError = fileEvaluation.getDiagnostics.toList
    val linterErrorFormatted = linterError
      .map { d =>
        d.position()
          .get()
          .formatMessage(d.severity().toString, d.message())
      }
      .mkString("\n\n")
      .replaceAllLiterally(main.toString, relativePath.toString)
      .replace('\\', '/') // for windows
    assertNoDiff(
      linterErrorFormatted,
      """|src/Main.scala:6:18: ERROR: semicolons are disabled
        |  println("test");
        |                 ^
      """.stripMargin
    )

    val unifiedDiff = fileEvaluation.previewPatchesAsUnifiedDiff.get()
    assert(unifiedDiff.nonEmpty)
    val patches = fileEvaluation.getPatches.toList

    val expectedWithOnePatch =
      """|
        |import scala.concurrent.Future
        |
        |object Main extends App {
        |  import scala.concurrent.Await
        |  println("test");
        |  println("ok")
        |}
        |""".stripMargin
    // if applying all patches we should get the same result
    val obtained2 =
      fileEvaluation.previewPatches(patches.toArray).get()
    assertNoDiff(obtained2, expected)

    val obtained3 = fileEvaluation
      .previewPatches(Seq(patches.head).toArray)
      .get
    assertNoDiff(obtained3, expectedWithOnePatch)

  }
  test("ScalafixArguments.evaluate getting StaleSemanticdb", SkipWindows) {
    val _ = scala.tools.nsc.Main.process(scalacOptions)
    val args = api
      .withRules(
        List(
          removeUnsuedRule().name.toString()
        ).asJava
      )
      .withClasspath((scalaLibrary.map(_.toNIO) :+ target).asJava)
      .withScalacOptions(Collections.singletonList(removeUnused))
      .withPaths(Seq(main).asJava)
      .withSourceroot(src)

    val result = args.evaluate()
    assert(result.getFileEvaluations.length == 1)
    assert(result.isSuccessful)
    // let's modify file and evaluate again
    val code = FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    val staleCode = code + "\n// comment\n"
    Files.write(main, staleCode.getBytes(StandardCharsets.UTF_8))
    val evaluation2 = args.evaluate()
    assert(!evaluation2.getError.isPresent)
    assert(!evaluation2.getMessageError.isPresent)
    assert(evaluation2.isSuccessful)
    val fileEval = evaluation2.getFileEvaluations.head
    assert(fileEval.getError.get.toString == "StaleSemanticdbError")
    assert(fileEval.getErrorMessage.get.startsWith("Stale SemanticDB"))
    assert(!fileEval.isSuccessful)
  }

  test(
    "ScalafixArguments.evaluate doesn't take into account withMode and withMainCallback",
    SkipWindows
  ) {
    val _ = scala.tools.nsc.Main.process(scalacOptions)
    val contentBeforeEvaluation =
      FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    var maybeDiagnostic: Option[ScalafixDiagnostic] = None
    val scalafixMainCallback = new ScalafixMainCallback {
      override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit =
        maybeDiagnostic = Some(diagnostic)
    }
    val result = api
      .withRules(
        List(
          removeUnsuedRule().name.toString(),
          "ExplicitResultTypes",
          "DisableSyntax"
        ).asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .withClasspath((scalaLibrary.map(_.toNIO) :+ target).asJava)
      .withScalacOptions(Collections.singletonList(removeUnused))
      .withPaths(Seq(main).asJava)
      .withSourceroot(src)
      .withMode(ScalafixMainMode.IN_PLACE)
      .withMainCallback(scalafixMainCallback)
      .evaluate()

    val fileEvaluation = result.getFileEvaluations.toSeq.head
    assert(fileEvaluation.getDiagnostics.toSeq.nonEmpty)
    assert(maybeDiagnostic.isEmpty)
    val content = FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    assert(contentBeforeEvaluation == content)
    val run = api
      .withRules(
        List(
          removeUnsuedRule().name.toString(),
          "ExplicitResultTypes",
          "DisableSyntax"
        ).asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .withClasspath((scalaLibrary.map(_.toNIO) :+ target).asJava)
      .withScalacOptions(Collections.singletonList(removeUnused))
      .withPaths(Seq(main).asJava)
      .withSourceroot(src)
      .withMode(ScalafixMainMode.IN_PLACE)
      .withMainCallback(scalafixMainCallback)
      .run()

    val contentAfterRun =
      FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    assert(contentAfterRun == fileEvaluation.previewPatches().get)
  }

  test("CommentFileNonAtomic retrieves 2 patches") {
    val run1 = api
      .withRules(List("CommentFileNonAtomic").asJava)
      .withSourceroot(src)

    val fileEvaluation1 = run1.evaluate().getFileEvaluations.head
    val patches1 = fileEvaluation1.getPatches
    assert(patches1.length == 2)

  }
  test("CommentFileAtomicRule retrieves 1 patch") {
    val run = api
      .withRules(List("CommentFileAtomic").asJava)
      .withSourceroot(src)
    val fileEvaluation = run.evaluate().getFileEvaluations.head
    val patches = fileEvaluation.getPatches
    assert(patches.length == 1)
  }

  test("Suppression mechanism isn't applied with non atomic patches") {
    val content =
      """|import scala.concurrent.duration // scalafix:ok
        |import scala.concurrent.Future""".stripMargin
    val cwd = StringFS
      .string2dir(
        s"""|/src/Main.scala
          |$content""".stripMargin,
        charset
      )
      .toNIO
    val src = cwd.resolve("src")
    val run = api
      .withRules(List("CommentFileNonAtomic").asJava)
      .withSourceroot(src)

    val fileEvaluation = run.evaluate().getFileEvaluations.head
    val obtained = fileEvaluation.previewPatches().get
    // A patch without `atomic` will ignore suppressions.
    val expected =
      """|/*import scala.concurrent.duration // scalafix:ok
        |import scala.concurrent.Future*/""".stripMargin
    assertNoDiff(obtained, expected)
  }

  test("Suppression mechanism is applied with atomic patches") {
    val content =
      """|import scala.concurrent.duration // scalafix:ok
        |import scala.concurrent.Future""".stripMargin
    val cwd = StringFS
      .string2dir(
        s"""|/src/Main.scala
          |$content""".stripMargin,
        charset
      )
      .toNIO
    val src = cwd.resolve("src")
    val run = api
      .withRules(List("CommentFileAtomic").asJava)
      .withSourceroot(src)

    val fileEvaluation = run.evaluate().getFileEvaluations.head
    val obtained = fileEvaluation.previewPatches().get
    assertNoDiff(obtained, content)
  }

  test(
    "Scalafix-evaluation-error-messages:Unknown rule error message",
    SkipWindows
  ) {
    val eval = api.withRules(List("nonExisting").asJava).evaluate()
    assert(!eval.isSuccessful)
    assert(eval.getError.get.toString == "CommandLineError")
    assert(eval.getMessageError.get.contains("Unknown rule"))
  }

  test("Scalafix-evaluation-error-messages: No file error", SkipWindows) {
    val eval = api
      .withPaths(Seq(Paths.get("/tmp/non-existing-file.scala")).asJava)
      .withRules(List("DisableSyntax").asJava)
      .evaluate()
    assert(!eval.isSuccessful)
    assert(eval.getError.get.toString == "NoFilesError")
    assert(eval.getMessageError.get == "No files to fix")
  }

  test("Scalafix-evaluation-error-messages: missing semanticdb", SkipWindows) {
    val eval = api
      .withPaths(Seq(main).asJava)
      .withRules(List("ExplicitResultTypes").asJava)
      .evaluate()
    assert(eval.isSuccessful)
    val fileEvaluation = eval.getFileEvaluations.head
    assert(fileEvaluation.getError.get.toString == "MissingSemanticdbError")
    assert(fileEvaluation.getErrorMessage.get.contains(main.toString))
    assert(fileEvaluation.getErrorMessage.get.contains("SemanticDB not found"))
  }

  test("withDialect Scala3") {
    val content =
      """|
        |object HelloWorld:
        |  @main def hello = println("Hello, world!")""".stripMargin
    val cwd = StringFS
      .string2dir(
        s"""|/src/Main.scala
          |$content""".stripMargin,
        charset
      )
      .toNIO
    val src = cwd.resolve("src")
    val run = api
      .withDialect(ScalafixDialect.Scala3)
      .withRules(List("CommentFileAtomic").asJava)
      .withSourceroot(src)
      .evaluate()

    val obtained = run.getFileEvaluations.head.previewPatches.get()

    val expected =
      """|/*
        |object HelloWorld:
        |  @main def hello = println("Hello, world!")*/""".stripMargin
    assertNoDiff(obtained, expected)
  }

  test("Source with Scala3 syntax cannot be parsed with dialect Scala2") {
    val content =
      """|
        |object HelloWorld:
        |  @main def hello = println("Hello, world!")""".stripMargin
    val cwd = StringFS
      .string2dir(
        s"""|/src/Main.scala
          |$content""".stripMargin,
        charset
      )
      .toNIO
    val src = cwd.resolve("src")
    val run = api
      .withDialect(ScalafixDialect.Scala2)
      .withRules(List("CommentFileAtomic").asJava)
      .withSourceroot(src)
      .evaluate()

    val obtainedError = run.getFileEvaluations.head.getError.get

    assert(obtainedError == ScalafixFileEvaluationError.ParseError)
  }
  def removeUnsuedRule(): SemanticRule = {
    val config = RemoveUnusedConfig.default
    new RemoveUnused(config)
  }

  def scalaLibrary: Seq[AbsolutePath] = Classpaths.scalaLibrary.entries

}
