package scalafix.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.immutable.Seq
import scalafix.internal.cli.CommonOptions
import scalafix.internal.cli.ScalafixOptions
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scala.meta.AbsolutePath
import ammonite.ops
import scala.meta.io.RelativePath
import scalafix.internal.rewrite.RemoveUnusedImports
import scalafix.testkit.SemanticRewriteSuite
import lang.meta.internal.io.FileIO
import org.scalatest.FunSuite

// extend this class to run custom cli tests.
trait BaseCliTest extends FunSuite with DiffAssertions {
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
  val devNull = CommonOptions(workingDirectory = cwd.toString)

  val default = ScalafixOptions(common = devNull)

  def check(
      name: String,
      originalLayout: String,
      args: Seq[String],
      expectedLayout: String,
      expectedExit: ExitStatus,
      outputAssert: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      val out = new ByteArrayOutputStream()
      val root = StringFS.string2dir(originalLayout)
      val exit =
        Cli.runMain(
          args,
          default.common.copy(
            workingDirectory = root.toString(),
            out = new PrintStream(out)
          ))
      val obtained = StringFS.dir2string(root)
      assertNoDiff(obtained, expectedLayout)
      outputAssert(out.toString)
      assert(exit == expectedExit)
    }
  }

  val semanticRoot: RelativePath = RelativePath("scala").resolve("test")
  val removeImportsPath: RelativePath =
    semanticRoot.resolve("RemoveUnusedImports.scala")
  def checkSemantic(
      name: String,
      args: Seq[String],
      expectedExit: ExitStatus,
      fileIsFixed: Boolean,
      outputAssert: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      val tmp = Files.createTempDirectory("scalafix")
      val out = new ByteArrayOutputStream()
      tmp.toFile.deleteOnExit()
      val root = ops.Path(tmp) / "input"
      ops.cp(ops.Path(BuildInfo.inputSourceroot.toPath), root)
      val exit = Cli.runMain(
        args ++ Seq(
          "-r",
          RemoveUnusedImports.toString(),
          removeImportsPath.toString()
        ),
        default.common.copy(
          workingDirectory = root.toString(),
          out = new PrintStream(out)
        )
      )
      val obtained = {
        val fixed =
          FileIO.slurp(AbsolutePath(root.toNIO).resolve(removeImportsPath))
        if (fileIsFixed) SemanticRewriteSuite.stripTestkitComments(fixed)
        else fixed
      }
      val expected =
        FileIO.slurp(
          AbsolutePath(
            if (fileIsFixed) BuildInfo.outputSourceroot
            else BuildInfo.inputSourceroot
          ).resolve(removeImportsPath))
      assertNoDiff(obtained, expected)
      outputAssert(out.toString())
      assert(exit == expectedExit)
    }
  }

  def parse(args: Seq[String]): CliCommand =
    Cli.parse(args, CommonOptions.default)
}
