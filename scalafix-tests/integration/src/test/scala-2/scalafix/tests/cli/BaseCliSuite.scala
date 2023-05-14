package scalafix.tests.cli

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.io.RelativePath

import org.scalatest.funsuite.AnyFunSuite
import scalafix.cli.ExitStatus
import scalafix.internal.tests.utils.SkipWindows
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.tests.BuildInfo
import scalafix.tests.util.ScalaVersions
import scalafix.v1.Main

// extend this class to run custom cli tests.
trait BaseCliSuite extends AnyFunSuite with DiffAssertions {

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

  val removeImportsPath: RelativePath =
    RelativePath("scala-2/test/removeUnused/RemoveUnusedImports.scala")
  val explicitResultTypesPath: RelativePath =
    RelativePath(
      "scala-2/test/explicitResultTypes/ExplicitResultTypesBase.scala"
    )

  def inputClasspath: Classpath = new Classpath(
    BuildInfo.inputClasspath.toList.map(f => AbsolutePath(f.getAbsolutePath))
  )
  def defaultClasspath: String = inputClasspath.syntax

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
        args,
        root.toNIO,
        new PrintStream(out)
      )
      val obtained = StringFS.dir2string(root)
      val output = fansi.Str(out.toString).plainText.replaceAll("\r\n", "\n")
      assert(exit == expectedExit, output)
      assertNoDiff(obtained, expectedLayout)
      outputAssert(output)
    }
  }

  def checkSuppress(
      name: String,
      originalFile: String,
      rule: String,
      expectedFile: String
  ): Unit = {
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
        "--check",
        "-r",
        rule,
        "a.scala"
      ),
      "/a.scala\n" + expectedFile,
      ExitStatus.Ok
    )
  }

  def slurp(path: AbsolutePath): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)
  def slurpInput(path: RelativePath): String =
    slurp(inputSourceroot.resolve(path))
  def slurpOutput(path: RelativePath): String =
    slurp(outputSourceroot.resolve(path))

  def copyRecursively(source: AbsolutePath, target: AbsolutePath): Unit = {
    Files.walkFileTree(
      source.toNIO,
      new SimpleFileVisitor[Path] {
        override def preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          val rel = source.toNIO.relativize(dir)
          Files.createDirectories(target.toNIO.resolve(rel))
          super.preVisitDirectory(dir, attrs)
        }
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          Files.copy(
            file,
            target.toNIO.resolve(source.toNIO.relativize(file)),
            StandardCopyOption.REPLACE_EXISTING
          )
          super.visitFile(file, attrs)
        }
      }
    )
  }

  val inputSourceroot: AbsolutePath =
    AbsolutePath(BuildInfo.inputSourceroot)
  val outputSourceroot: AbsolutePath =
    AbsolutePath(BuildInfo.inputSourceroot)

  case class Result(
      exit: ExitStatus,
      original: String,
      obtained: String,
      expected: String
  )
  def checkSemantic(
      name: String,
      args: Array[String],
      targetroots: Seq[String] =
        BuildInfo.inputSemanticClasspath.map(_.getAbsolutePath),
      expectedExit: ExitStatus,
      preprocess: AbsolutePath => Unit = _ => (),
      outputAssert: String => Unit = _ => (),
      rule: String = "RemoveUnused",
      path: RelativePath = removeImportsPath,
      files: String = removeImportsPath.toString()
  ): Unit = {
    test(name, SkipWindows) {
      val fileIsFixed = expectedExit.isOk
      val cwd = Files.createTempDirectory("scalafix")
      val sourceDir =
        inputSourceroot.toRelative(AbsolutePath(BuildInfo.baseDirectory)).toNIO
      val inputSourceDirectory = cwd.resolve(sourceDir)
      Files.createDirectories(inputSourceDirectory)
      val root = AbsolutePath(inputSourceDirectory)
      val out = new ByteArrayOutputStream()
      root.toFile.deleteOnExit()
      copyRecursively(source = inputSourceroot, target = root)
      preprocess(root)
      val sourceroot =
        if (args.contains("--sourceroot")) Array[String]()
        else Array("--sourceroot", cwd.toString)
      val targetroots0 =
        targetroots.flatMap(Seq("--semanticdb-targetroots", _))
      val scalaOption =
        if (ScalaVersions.isScala213)
          "-Wunused:imports"
        else "-Ywarn-unused-import"
      val allArguments = args ++ sourceroot ++ targetroots0 ++ Seq(
        "--scalac-options",
        scalaOption,
        "-r",
        rule,
        files
      )
      val exit = Main.run(allArguments, root.toNIO, new PrintStream(out))
      val output = fansi.Str(out.toString()).plainText
      assert(exit == expectedExit, output)
      outputAssert(output)
    }
  }

  def runMain(args: Array[String], cwd: Path): (String, ExitStatus) = {
    val out = new ByteArrayOutputStream()
    val exit = Main.run(args, cwd, new PrintStream(out))
    (fansi.Str(out.toString()).plainText, exit)
  }

}
