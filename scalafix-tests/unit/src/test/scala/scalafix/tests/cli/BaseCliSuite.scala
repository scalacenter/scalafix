package scalafix.tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.immutable.Seq
import scala.meta.internal.io.FileIO
import scalafix.cli.ExitStatus
import scalafix.internal.rule.RemoveUnusedImports
import scalafix.internal.tests.utils.SkipWindows
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.testkit.SemanticRuleSuite
import scalafix.tests.BuildInfo
import ammonite.ops
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import org.scalatest.FunSuite
import scala.meta.io.Classpath
import scalafix.v1.Main

// extend this class to run custom cli tests.
trait BaseCliSuite extends FunSuite with DiffAssertions {

  val original: String =
    """|object Main {
       |  def foo() {
       |  }
       |}
       |""".stripMargin
  val expected: String =
    """|object Main {
       |  def foo(): Unit = {
       |  }
       |}
       |""".stripMargin
  val cwd: Path = Files.createTempDirectory("scalafix-cli")
  val ps = new PrintStream(new ByteArrayOutputStream())

  val semanticRoot: RelativePath = RelativePath("scala").resolve("test")
  val removeImportsPath: RelativePath =
    semanticRoot.resolve("RemoveUnusedImports.scala")
  val explicitResultTypesPath: RelativePath =
    semanticRoot
      .resolve("explicitResultTypes")
      .resolve("ExplicitResultTypesBase.scala")
  val semanticClasspath: String =
    BuildInfo.semanticClasspath.getAbsolutePath
  def defaultClasspath: Classpath =
    SemanticRuleSuite.defaultClasspath(
      AbsolutePath(BuildInfo.semanticClasspath))

  def check(
      name: String,
      originalLayout: String,
      args: Array[String],
      expectedLayout: String,
      expectedExit: ExitStatus,
      outputAssert: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      val out = new ByteArrayOutputStream()
      val root = StringFS.string2dir(originalLayout)

      val exit = Main.run(
        Array(
          "--sourceroot",
          root.toString(),
          "--classpath",
          semanticClasspath
        ) ++ args,
        root.toNIO,
        new PrintStream(out)
      )
      val obtained = StringFS.dir2string(root)
      val output = fansi.Str(out.toString).plainText
      assert(exit == expectedExit, output)
      assertNoDiff(obtained, expectedLayout)
      outputAssert(output)
    }
  }

  def checkSuppress(
      name: String,
      originalFile: String,
      rule: String,
      expectedFile: String): Unit = {
    check(
      name,
      "/a.scala\n" + originalFile,
      Array(
        "--auto-suppress-linter-errors",
        "-r",
        rule,
        "a.scala"
      ),
      "/a.scala\n" + expectedFile,
      ExitStatus.Ok
    )
    check(
      "check " + name,
      "/a.scala\n" + expectedFile,
      Array(
        "--test",
        "-r",
        rule,
        "a.scala"
      ),
      "/a.scala\n" + expectedFile,
      ExitStatus.Ok
    )
  }

  def writeTestkitConfiguration(root: Path, path: Path): Unit = {
    import scala.meta._
    val code = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    val comment = SemanticRuleSuite.findTestkitComment(code.tokenize.get)
    val content = comment.syntax.stripPrefix("/*").stripSuffix("*/")
    Files.write(
      root.resolve(".scalafix.conf"),
      content.getBytes(StandardCharsets.UTF_8)
    )
  }

  def slurp(path: AbsolutePath): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)
  def slurpInput(path: RelativePath): String =
    slurp(AbsolutePath(BuildInfo.inputSourceroot.toPath).resolve(path))
  def slurpOutput(path: RelativePath): String =
    slurp(AbsolutePath(BuildInfo.outputSourceroot.toPath).resolve(path))

  case class Result(
      exit: ExitStatus,
      original: String,
      obtained: String,
      expected: String)
  def checkSemantic(
      name: String,
      args: Array[String],
      expectedExit: ExitStatus,
      preprocess: AbsolutePath => Unit = _ => (),
      outputAssert: String => Unit = _ => (),
      rule: String = RemoveUnusedImports.toString(),
      path: RelativePath = removeImportsPath,
      files: String = removeImportsPath.toString(),
      assertObtained: Result => Unit = { result =>
        if (result.exit.isOk) {
          assertNoDiff(result.obtained, result.expected)
        }
      }
  ): Unit = {
    test(name, SkipWindows) {
      val fileIsFixed = expectedExit.isOk
      val tmp = Files.createTempDirectory("scalafix")
      val out = new ByteArrayOutputStream()
      tmp.toFile.deleteOnExit()
      val root = ops.Path(tmp) / "input"
      ops.cp(ops.Path(BuildInfo.inputSourceroot.toPath), root)
      val rootNIO = root.toNIO
      writeTestkitConfiguration(rootNIO, rootNIO.resolve(path.toNIO))
      preprocess(AbsolutePath(rootNIO))
      val exit = Main.run(
        args ++ Seq(
          "-r",
          rule,
          files
        ),
        root.toNIO,
        new PrintStream(out)
      )
      val original = slurpInput(path)
      val obtained = {
        val fixed =
          FileIO.slurp(
            AbsolutePath(root.toNIO).resolve(path),
            StandardCharsets.UTF_8)
        if (fileIsFixed && fixed.startsWith("/*")) {
          SemanticRuleSuite.stripTestkitComments(fixed)
        } else {
          fixed
        }

      }
      val expected =
        if (fileIsFixed) slurpOutput(path)
        else original
      val output = fansi.Str(out.toString()).plainText
      assert(exit == expectedExit, output)
      outputAssert(output)
      val result = Result(exit, original, obtained, expected)
      assertObtained(result)
    }
  }

}
