package scalafix.tests.interfaces

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional

import scala.collection.JavaConverters._
import scala.util.Try

import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

import org.scalactic.source
import org.scalatest.Tag
import org.scalatest.funsuite.AnyFunSuite
import scalafix.interfaces.ScalafixArguments
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixException
import scalafix.interfaces.ScalafixMainCallback
import scalafix.interfaces.ScalafixMainMode
import scalafix.interfaces.ScalafixPatch
import scalafix.internal.interfaces.ScalafixArgumentsImpl
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scalafix.tests.core.Classpaths
import scalafix.tests.util.ScalaVersions
import scalafix.tests.util.compat.CompatSemanticdb

class ScalafixArgumentsSuite extends AnyFunSuite with DiffAssertions {

  val scalaVersion: String = BuildInfo.scalaVersion
  val scalaLibrary: Seq[AbsolutePath] = Classpaths.scalaLibrary.entries

  val rawApi: ScalafixArguments = ScalafixArgumentsImpl()

  // fixturing variant of test, allowing to customize created input files
  def fsTest[T](testName: String, testTags: Tag*)(
      string2dir: String = """|/src/Main.scala
        |import scala.concurrent.duration
        |import scala.concurrent.Future
        |
        |final object Main extends App {
        |  import scala.concurrent.Await
        |  println("test");
        |  println("ok")
        |}""".stripMargin,
      charset: Charset = StandardCharsets.UTF_8,
      sourcesToCompile: Seq[String] = Seq("Main.scala")
  )(
      testFun: (ScalafixArguments, Path) => Any
  )(implicit pos: source.Position): Unit =
    test(testName, testTags: _*) {
      // Current Working Directory
      val cwd: Path = StringFS.string2dir(string2dir, charset).toNIO

      // input
      val src: Path = cwd.resolve("src")
      // compiler + semanticdb output
      val d: Path = cwd.resolve("out")
      Files.createDirectories(d)

      val scalacOptions = {
        val reportUnusedOption =
          if (ScalaVersions.isScala212) "-Ywarn-unused-import"
          else "-Wunused:imports"
        val semanticDbOptions = CompatSemanticdb.scalacOptions(src)
        val sourcesOptions = sourcesToCompile.map(src.resolve).map(_.toString)
        List(
          "-classpath",
          s"${scalaLibrary.mkString(File.pathSeparator)}",
          "-d",
          d.toString,
          reportUnusedOption
        ) ++ semanticDbOptions ++ sourcesOptions
      }

      if (sourcesToCompile.nonEmpty) {
        CompatSemanticdb.runScalac(scalacOptions)
      }

      val api = rawApi
        .withCharset(charset)
        .withWorkingDirectory(cwd)
        .withSourceroot(src)
        .withClasspath((d +: scalaLibrary.map(_.toNIO)).asJava)
        .withPaths(sourcesToCompile.map(src.resolve).asJava)
        .withScalacOptions(scalacOptions.asJava)
        .withScalaVersion(scalaVersion)

      testFun(api, cwd)
    }

  test("availableRules") {
    val rules = rawApi.availableRules().asScala
    val names = rules.map(_.name())
    assert(names.contains("DisableSyntax"))
    assert(names.contains("AvailableRule"))
    assert(!names.contains("DeprecatedAvailableRule"))
    val hasDescription = rules.filter(_.description().nonEmpty)
    assert(hasDescription.nonEmpty)
    val isSyntactic = rules.filter(_.kind().isSyntactic)
    assert(isSyntactic.nonEmpty)
    val isSemantic = rules.filter(_.kind().isSemantic)
    assert(isSemantic.nonEmpty)
    val isLinter = rules.filter(_.isLinter)
    assert(isLinter.nonEmpty)
    val isRewrite = rules.filter(_.isRewrite)
    assert(isRewrite.nonEmpty)
    val isExperimental = rules.filter(_.isExperimental)
    assert(isExperimental.isEmpty)
  }

  test("validate allows rules to check for scalacOptions") {
    val redundantSyntax =
      rawApi.withRules(List("RedundantSyntax").asJava).validate()
    assert(!redundantSyntax.isPresent)

    val removeUnused =
      rawApi.withRules(List("RemoveUnused").asJava).validate()
    assert(removeUnused.isPresent)
    assert(removeUnused.get().getMessage.contains("-Wunused"))
  }

  fsTest("rulesThatWillRun")(
    """|/.scalafix.conf
      |rules = ["DisableSyntax"]
  """.stripMargin,
    sourcesToCompile = Nil
  ) { case (api, _) =>
    api.validate()

    assert(
      api.rulesThatWillRun().asScala.toList.map(_.toString) == List(
        "ScalafixRule(DisableSyntax)"
      )
    )

    // if a non empty list of rules is provided, rules from config file are ignored
    val withExplicitRule = api.withRules(List("RedundantSyntax").asJava)
    withExplicitRule.validate()

    assert(
      withExplicitRule.rulesThatWillRun().asScala.toList.map(_.name()) == List(
        "RedundantSyntax"
      )
    )

  }

  fsTest("run syntactic/semantic & built-in/external rules at the same time")(
    """|/src/Semicolon.scala
      |
      |object Semicolon {
      |  val a = 1; // みりん þæö
      |  import scala.concurrent.Future
      |  def main = { println(s"42") }
      |}
      |
      |/src/Excluded.scala
      |object Excluded {
      |  val a = 1;
      |}
      """.stripMargin,
    StandardCharsets.US_ASCII, // Assert that non-ascii characters read into "?"
    Seq("Semicolon.scala", "Excluded.scala")
  ) { case (api, cwd) =>
    val buf = List.newBuilder[ScalafixDiagnostic]
    val callback = new ScalafixMainCallback {
      override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit = {
        buf += diagnostic
      }
    }
    val out = new ByteArrayOutputStream()
    val args = api
      .withToolClasspath(
        Nil.asJava,
        Seq(s"ch.epfl.scala::example-scalafix-rule:1.6.0").asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .withExcludedPaths(
        List(
          FileSystems.getDefault.getPathMatcher("glob:**Excluded.scala")
        ).asJava
      )
      .withMainCallback(callback)
      .withRules(
        List(
          "DisableSyntax", // syntactic linter
          "RedundantSyntax", // syntactic rewrite
          "RemoveUnused", // semantic rewrite
          "class:fix.Examplescalafixrule_v1" // --tool-classpath
        ).asJava
      )
      .withPrintStream(new PrintStream(out))
      .withMode(ScalafixMainMode.CHECK)
    val expectedRulesToRun = List(
      "RedundantSyntax",
      "RemoveUnused",
      "ExampleScalafixRule_v1",
      "DisableSyntax"
    )
    val obtainedRulesToRun =
      args.rulesThatWillRun().asScala.toList.map(_.name())
    assertNoDiff(
      obtainedRulesToRun.sorted.mkString("\n"),
      expectedRulesToRun.sorted.mkString("\n")
    )
    val validateError: Optional[ScalafixException] = args.validate()
    assert(!validateError.isPresent, validateError)
    val scalafixErrors = args.run()
    val errors = scalafixErrors.toList.map(_.toString).sorted
    val stdout = fansi
      .Str(out.toString(StandardCharsets.US_ASCII.name()))
      .plainText
      .replaceAllLiterally(cwd.toString, "")
      .replace('\\', '/') // for windows
      .linesIterator
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
      .replaceAllLiterally(cwd.toString, "")
      .replace('\\', '/') // for windows
    assertNoDiff(
      linterDiagnostics,
      """|/src/Semicolon.scala:3:12: ERROR: semicolons are disabled
        |  val a = 1; // ??? ???
        |           ^
      """.stripMargin
    )
    assertNoDiff(
      stdout,
      """|--- /src/Semicolon.scala
        |+++ <expected fix>
        |@@ -1,6 +1,6 @@
        | object Semicolon {
        |   val a = 1; // ??? ???
        |-  import scala.concurrent.Future
        |-  def main = { println(s"42") }
        |+  def main = { println("42") }
        | }
        |+// Hello world!
        |""".stripMargin
    )
  }

  fsTest("evaluate with a semantic rule")() { case (api, cwd) =>
    val eval = api
      .withRules(
        List(
          "RemoveUnused",
          "DisableSyntax"
        ).asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .evaluate()

    val error = eval.getError
    assert(!error.isPresent) // we ignore completely linterErrors
    assert(eval.isSuccessful)
    assert(eval.getFileEvaluations.length == 1)
    val fileEvaluation = eval.getFileEvaluations.head
    assert(fileEvaluation.isSuccessful)

    val linterError = fileEvaluation.getDiagnostics.toList
    val linterErrorFormatted = linterError
      .map { d =>
        d.position()
          .get()
          .formatMessage(d.severity().toString, d.message())
      }
      .mkString("\n\n")
      .replaceAllLiterally(cwd.toString, "")
      .replace('\\', '/') // for windows
    assertNoDiff(
      linterErrorFormatted,
      """|/src/Main.scala:6:18: ERROR: semicolons are disabled
        |  println("test");
        |                 ^
      """.stripMargin
    )

    val expected =
      """|
        |final object Main extends App {
        |  println("test");
        |  println("ok")
        |}
        |""".stripMargin
    val globalPreview = fileEvaluation.previewPatches.get()
    assertNoDiff(globalPreview, expected)

    val globalPreviewAsUnifiedDiff =
      fileEvaluation.previewPatchesAsUnifiedDiff.get()
    assert(globalPreviewAsUnifiedDiff.nonEmpty)

    val allPatches = fileEvaluation.getPatches.toList
    val previewAllPatches =
      fileEvaluation.previewPatches(allPatches.toArray).get()
    assertNoDiff(previewAllPatches, expected)

    val expectedOnePatch =
      """|
        |import scala.concurrent.Future
        |
        |final object Main extends App {
        |  import scala.concurrent.Await
        |  println("test");
        |  println("ok")
        |}
        |""".stripMargin
    val previewOnePatch = fileEvaluation
      .previewPatches(Seq(allPatches.head).toArray)
      .get
    assertNoDiff(previewOnePatch, expectedOnePatch)

  }

  fsTest("evaluate with StaleSemanticdb")() { case (api, cwd) =>
    val args = api.withRules(List("RemoveUnused").asJava)
    val result = args.evaluate()
    assert(result.getFileEvaluations.length == 1)
    assert(result.isSuccessful)

    // let's modify file
    val main = cwd.resolve("src/Main.scala")
    val code = FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    val staleCode = code + "\n// comment\n"
    Files.write(main, staleCode.getBytes)

    // and evaluate again
    val evaluation2 = args.evaluate()
    assert(!evaluation2.getError.isPresent)
    assert(!evaluation2.getErrorMessage.isPresent)
    assert(evaluation2.isSuccessful)
    val fileEval = evaluation2.getFileEvaluations.head
    assert(fileEval.getError.get.toString == "StaleSemanticdbError")
    assert(fileEval.getErrorMessage.get.startsWith("Stale SemanticDB"))
    assert(!fileEval.isSuccessful)
  }

  fsTest("evaluate ignores mode/callback")() { case (api, cwd) =>
    val main = cwd.resolve("src/Main.scala")
    val contentBeforeEvaluation =
      FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)

    var maybeDiagnostic: Option[ScalafixDiagnostic] = None
    val scalafixMainCallback = new ScalafixMainCallback {
      override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit =
        maybeDiagnostic = Some(diagnostic)
    }
    val args = api
      .withRules(
        List(
          "RedundantSyntax",
          "DisableSyntax"
        ).asJava
      )
      .withParsedArguments(
        List("--settings.DisableSyntax.noSemicolons", "true").asJava
      )
      .withMode(ScalafixMainMode.IN_PLACE)
      .withMainCallback(scalafixMainCallback)

    val evaluation = args.evaluate()
    val fileEvaluation = evaluation.getFileEvaluations.toSeq.head
    // there is a diagnostic ...
    assert(fileEvaluation.getDiagnostics.toSeq.nonEmpty)
    // ... but scalafixMainCallback was ignored since we we didn't track any
    assert(maybeDiagnostic.isEmpty)
    val content = FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    // ScalafixMainMode was ignored because rewrites were not applied
    assert(contentBeforeEvaluation == content)

    args.run()
    val contentAfterRun =
      FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    assert(contentAfterRun == fileEvaluation.previewPatches().get)
  }

  test("evaluate sees several patches for non-atomic patches") {
    val run = rawApi.withRules(List("CommentFileNonAtomic").asJava)
    val fileEvaluation = run.evaluate().getFileEvaluations.head
    val patches = fileEvaluation.getPatches
    assert(patches.length == 2)
  }

  test("evaluate sees a single patch for atomic patches") {
    val args = rawApi.withRules(List("CommentFileAtomic").asJava)
    val fileEvaluation = args.evaluate().getFileEvaluations.head
    val patches = fileEvaluation.getPatches
    assert(patches.length == 1)
  }

  fsTest("evaluate ignores suppression mechanism with non-atomic patches")(
    """|/src/Main.scala
      |import scala.concurrent.duration // scalafix:ok
      |import scala.concurrent.Future""".stripMargin
  ) { case (api, _) =>
    val args = api.withRules(List("CommentFileNonAtomic").asJava)

    val fileEvaluation = args.evaluate().getFileEvaluations.head
    val obtained = fileEvaluation.previewPatches().get
    val expected =
      """|/*import scala.concurrent.duration // scalafix:ok
        |import scala.concurrent.Future*/""".stripMargin
    assertNoDiff(obtained, expected)
  }

  fsTest("evaluate honors suppression mechanism with atomic patches")(
    """|/src/Main.scala
      |import scala.concurrent.duration // scalafix:ok
      |import scala.concurrent.Future""".stripMargin
  ) { case (api, cwd) =>
    val main = cwd.resolve("src/Main.scala")
    val content = FileIO.slurp(AbsolutePath(main), StandardCharsets.UTF_8)
    val args = api.withRules(List("CommentFileAtomic").asJava)

    val fileEvaluation = args.evaluate().getFileEvaluations.head
    val obtained = fileEvaluation.previewPatches().get
    assertNoDiff(obtained, content)
  }

  test("evaluate with non-existing rule") {
    val eval = rawApi.withRules(List("nonExisting").asJava).evaluate()
    assert(!eval.isSuccessful)
    assert(eval.getError.get.toString == "CommandLineError")
    assert(eval.getErrorMessage.get.contains("Unknown rule"))
  }

  test("evaluate with no file provided") {
    val eval = rawApi
      .withPaths(Seq(Paths.get("/tmp/non-existing-file.scala")).asJava)
      .withRules(List("DisableSyntax").asJava)
      .evaluate()
    assert(!eval.isSuccessful)
    assert(eval.getError.get.toString == "NoFilesError")
    assert(eval.getErrorMessage.get == "No files to fix")
  }

  test("evaluate with no rule provided") {
    // don't get rules from the project's .scalafix.conf
    val noscalafixconfig = Files.createTempDirectory("scalafix")
    val eval = rawApi
      .withWorkingDirectory(noscalafixconfig)
      .evaluate()
    assert(!eval.isSuccessful)
    assert(eval.getErrorMessage.get == "No rules requested to run")
  }

  fsTest("evaluate with missing semanticdb")(
    sourcesToCompile = Nil // don't compile anything
  ) { case (api, _) =>
    val eval = api
      .withRules(List("RemoveUnused").asJava)
      .evaluate()
    assert(eval.isSuccessful)
    val fileEvaluation = eval.getFileEvaluations.head
    assert(fileEvaluation.getError.get.toString == "MissingSemanticdbError")
    assert(fileEvaluation.getErrorMessage.get.contains("Main.scala"))
    assert(fileEvaluation.getErrorMessage.get.contains("SemanticDB not found"))
  }

  test(
    "Scala2 source with Scala3 syntax can only be parsed with -Xsource:3 or -Xsource:3-cross"
  ) {
    val cwd = StringFS
      .string2dir(
        s"""|/src/Scala2Source3.scala
          |open class Scala2Source3""".stripMargin
      )
      .toNIO
    val src = cwd.resolve("src")
    val args = rawApi
      .withScalaVersion("2.13.6")
      .withRules(List("DisableSyntax").asJava)
      .withSourceroot(src)

    val withoutSource3 = args.evaluate().getFileEvaluations.head
    assert(!withoutSource3.isSuccessful)

    val withSource3 =
      args
        .withScalacOptions(List("-Xsource:3").asJava)
        .evaluate()
        .getFileEvaluations
        .head
    assert(withSource3.isSuccessful)

    val withSource3cross =
      args
        .withScalacOptions(List("-Xsource:3-cross").asJava)
        .evaluate()
        .getFileEvaluations
        .head
    assert(withSource3cross.isSuccessful)
  }

  test("Scala 2.11 is no longer supported") {
    val run = Try(rawApi.withScalaVersion("2.11.12"))
    val expectedErrorMessage =
      "Scala 2.11 is no longer supported; the final version supporting it is Scalafix 0.10.4"
    assert(run.failed.toOption.map(_.getMessage) == Some(expectedErrorMessage))
  }

  fsTest("textEdits returns patch as edits")() { case (api, _) =>
    val run = api
      .withRules(
        List(
          "CommentFileNonAtomic",
          "CommentFileAtomic"
        ).asJava
      )

    val fileEvaluation = run.evaluate().getFileEvaluations.head
    val patches = fileEvaluation.getPatches

    // CommentFileNonAtomic produces two patches which in turn produce one
    // token patch each, whilst CommentFileAtomic produces one patch which
    // in turn produces two token patches
    val Array(nonAtomicEditArray1, nonAtomicEditArray2, atomicEditArray) =
      patches.map(_.textEdits()).sortBy(_.length)

    // Check the above holds
    assert(nonAtomicEditArray1.length == 1 && nonAtomicEditArray2.length == 1)
    assert(atomicEditArray.length == 2)

    // Check the offsets for the atomic edits look ok (e.g. they're zero-based)
    val Array(atomicEdit1, atomicEdit2) =
      atomicEditArray.sortBy(_.position.startLine)
    assert(
      atomicEdit1.position.startLine == 0 && atomicEdit1.position.startColumn == 0
    )
    assert(
      atomicEdit2.position.startLine == 7 && atomicEdit2.position.startColumn == 1
    )

    // Check the same holds for the non-atomic edit pair
    val Array(nonAtomicEdit1, nonAtomicEdit2) =
      (nonAtomicEditArray1 ++ nonAtomicEditArray2).sortBy(_.position.startLine)
    assert(
      nonAtomicEdit1.position.startLine == 0 && nonAtomicEdit1.position.startColumn == 0
    )
    assert(
      nonAtomicEdit2.position.startLine == 7 && nonAtomicEdit2.position.startColumn == 1
    )
  }

  fsTest("ScalafixPatch isAtomic")() { case (api, _) =>
    def patches(rules: List[String]): Array[ScalafixPatch] = {
      val run = api.withRules(rules.asJava)
      val fileEvaluation = run.evaluate().getFileEvaluations.head
      fileEvaluation.getPatches
    }

    assert(patches(List("CommentFileAtomic")).forall(_.isAtomic))
    assert(patches(List("CommentFileNonAtomic")).forall(!_.isAtomic))
  }

  test("com.github.liancheng::organize-imports is ignored") {
    val rules = rawApi
      .withToolClasspath(
        Nil.asJava,
        Seq("com.github.liancheng::organize-imports:0.6.0").asJava
      )
      .availableRules()
      .asScala
      .filter(_.name() == "OrganizeImports")

    // only one should be loaded
    assert(rules.length == 1)

    // ensure it's the built-in one (the external one was marked as experimental)
    assert(!rules.head.isExperimental)
  }

}
