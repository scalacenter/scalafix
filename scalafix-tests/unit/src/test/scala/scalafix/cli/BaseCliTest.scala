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
  val devNull = CommonOptions(
    // Uncomment these lines to diagnose errors from tests.
    out = ps,
    err = ps,
    workingDirectory = cwd.toString
  )

  val default = ScalafixOptions(common = devNull)

  def check(
      name: String,
      originalLayout: String,
      args: Seq[String],
      expectedLayout: String,
      expectedExit: ExitStatus,
      common: CommonOptions = devNull
  ): Unit = {
    test(name) {
      val root = StringFS.string2dir(originalLayout)
      val exit =
        Cli.runMain(args, common.copy(workingDirectory = root.toString()))
      assert(exit == expectedExit)
      val obtained = StringFS.dir2string(root)
      assertNoDiff(obtained, expectedLayout)
    }
  }
  def parse(args: Seq[String]): CliCommand =
    Cli.parse(args, CommonOptions.default)
}
