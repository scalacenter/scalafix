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
import scala.meta.io.RelativePath

import org.scalatest.funsuite.AnyFunSuite
import scalafix.cli.ExitStatus
import scalafix.internal.tests.utils.SkipWindows
import scalafix.test.StringFS
import scalafix.testkit.DiffAssertions
import scalafix.testkit.SemanticRuleSuite
import scalafix.testkit.TestkitProperties
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

  val semanticRoot: RelativePath = RelativePath("test")
  val removeImportsPath: RelativePath =
    semanticRoot.resolve("removeUnused/RemoveUnusedImports.scala")
  val explicitResultTypesPath: RelativePath =
    semanticRoot
      .resolve("explicitResultTypes")
      .resolve("ExplicitResultTypesBase.scala")
  val props: TestkitProperties = TestkitProperties.loadFromResources()
  def defaultClasspath: String =
    props.inputClasspath.syntax

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

  def writeTestkitConfiguration(
      root: AbsolutePath,
      path: AbsolutePath
  ): Unit = {
    import scala.meta._
    val code = slurp(path)
    val comment = SemanticRuleSuite.findTestkitComment(code.tokenize.get)
    val content = comment.syntax.stripPrefix("/*").stripSuffix("*/")
    Files.write(
      root.resolve(".scalafix.conf").toNIO,
      content.getBytes(StandardCharsets.UTF_8)
    )
  }

  def slurp(path: AbsolutePath): String =
    FileIO.slurp(path, StandardCharsets.UTF_8)
  def slurpInput(path: RelativePath): String =
    slurp(props.resolveInput(path))
  def slurpOutput(path: RelativePath): String =
    slurp(props.resolveOutput(path))

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

  def sourceDirectory: AbsolutePath =
    props.inputSourceDirectories
      // TODO: This test won't work for scala 3. The path is hardcoded
      .find(dir => dir.toNIO.endsWith("scala-2")) // Skip scala-2.12
      .getOrElse {
        throw new IllegalArgumentException(
          props.inputSourceDirectories.toString()
        )
      }

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
        BuildInfo.semanticClasspath.map(_.getAbsolutePath),
      expectedExit: ExitStatus,
      preprocess: AbsolutePath => Unit = _ => (),
      outputAssert: String => Unit = _ => (),
      rule: String = "RemoveUnused",
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
      val cwd = Files.createTempDirectory("scalafix")
      val inputSourceDirectory =
        cwd.resolve("scalafix-tests/input/src/main/scala-2/")
      Files.createDirectories(inputSourceDirectory)
      val root = AbsolutePath(inputSourceDirectory)
      val out = new ByteArrayOutputStream()
      root.toFile.deleteOnExit()
      copyRecursively(source = sourceDirectory, target = root)
      val rootNIO = root
      writeTestkitConfiguration(rootNIO, rootNIO.resolve(path))
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
      val original = slurpInput(path)
      val obtained = {
        val fixed = slurp(root.resolve(path))
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

  def runMain(args: Array[String], cwd: Path): (String, ExitStatus) = {
    val out = new ByteArrayOutputStream()
    val exit = Main.run(args, cwd, new PrintStream(out))
    (fansi.Str(out.toString()).plainText, exit)
  }

}
