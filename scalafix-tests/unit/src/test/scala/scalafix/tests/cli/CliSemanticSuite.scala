package scalafix.tests.cli

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.Classpath
import scala.meta.testkit.StringFS

import scalafix.cli._
import scalafix.tests.core.Classpaths

class CliSemanticSuite extends BaseCliSuite {

  checkSemantic(
    name = "--classpath ok",
    args = Array(
      "--classpath",
      defaultClasspath
    ),
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "--auto-classpath ok",
    args = Array(
      "--auto-classpath-roots",
      PathIO.workingDirectory.toString(),
      "--auto-classpath"
    ),
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "--sourceroot not ok",
    args = Array(
      "--sourceroot",
      "bogus",
      "--classpath",
      defaultClasspath
    ),
    expectedExit = ExitStatus.CommandLineError,
    outputAssert = { out =>
      assert(out.contains("--sourceroot"))
      assert(out.contains("bogus"))
    }
  )

  checkSemantic(
    name = "missing --classpath",
    args = Array(),
    expectedExit = ExitStatus.MissingSemanticdbError
  )

  test("MissingSemanticDB") {
    val cwd = Files.createTempDirectory("scalafix")
    val name = "MissingSemanticDB.scala"
    Files.createFile(cwd.resolve(name))
    val (out, exit) = runMain(
      Array(
        // no --classpath
        "--scalac-options",
        "-Ywarn-unused-import",
        "-r",
        "RemoveUnused",
        name
      ),
      cwd
    )
    assert(exit.is(ExitStatus.MissingSemanticdbError), exit.toString)
    assert(out.contains("SemanticDB not found: "))
    assert(out.contains(name))
  }

  checkSemantic(
    name = "StaleSemanticDB",
    args = Array(
      "--classpath",
      defaultClasspath
    ),
    preprocess = { root =>
      val path = root.resolve(explicitResultTypesPath)
      val code = FileIO.slurp(path, StandardCharsets.UTF_8)
      val staleCode = code + "\n// comment\n"
      Files.write(path.toNIO, staleCode.getBytes(StandardCharsets.UTF_8))
    },
    expectedExit = ExitStatus.StaleSemanticdbError,
    rule = "ExplicitResultTypes",
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString(),
    outputAssert = { out =>
      assert(out.contains("Stale SemanticDB"))
      assert(
        out.contains(
          explicitResultTypesPath.toString + "-ondisk-md5-fingerprint"
        )
      )
    }
  )

  checkSemantic(
    name = "StaleSemanticDB fix matches input",
    args = Array(
      "--classpath",
      defaultClasspath
    ),
    preprocess = { root =>
      val expectedOutput = slurpOutput(explicitResultTypesPath)
      val path = root.resolve(explicitResultTypesPath)
      Files.write(path.toNIO, expectedOutput.getBytes(StandardCharsets.UTF_8))
    },
    expectedExit = ExitStatus.Ok,
    rule = "ExplicitResultTypes",
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString(),
    outputAssert = { out =>
      assert(out.isEmpty)
    }
  )

  checkSemantic(
    name = "explicit result types OK",
    args = Array(
      "--classpath",
      defaultClasspath
    ),
    expectedExit = ExitStatus.Ok,
    rule = "ExplicitResultTypes",
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString()
  )

  checkSemantic(
    name = "explicit result types OK (auto-classpath)",
    args = Array(
      "--auto-classpath",
      "--auto-classpath-roots",
      PathIO.workingDirectory.toString
    ) ++ Classpaths.scalaLibrary.entries.toArray.flatMap { path =>
      Array(
        "--auto-classpath-roots",
        path.toString
      )
    },
    expectedExit = ExitStatus.Ok,
    rule = "ExplicitResultTypes",
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString()
  )

  checkSemantic(
    name = "incomplete classpath does not result in error exit code",
    args = Array(
      "--classpath",
      Classpath(
        props.inputClasspath.entries
          .filterNot(_.toString().contains("scala-library"))
      ).syntax
    ),
    // Errors in ExplicitResultTypes are suppressed.
    expectedExit = ExitStatus.Ok,
    rule = "ExplicitResultTypes",
    path = explicitResultTypesPath,
    assertObtained = { _ =>
      // Do nothing
    }
  )

  checkSemantic(
    name = "-P:semanticdb:targetroot",
    args = {
      val (_ :: targetroot :: Nil, jars) =
        props.inputClasspath.entries.partition(_.isDirectory)
      Array(
        s"--scalacOptions",
        s"-P:semanticdb:targetroot:shouldBeIgnored",
        s"--scalacOptions",
        s"-P:semanticdb:targetroot:${targetroot.toString()}",
        "--classpath",
        Classpath(jars).syntax
      )
    },
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "-P:semanticdb:include",
    args = Array(
      s"--scalacOptions",
      s"-P:semanticdb:include:${removeImportsPath.toNIO.getFileName}",
      "--classpath",
      defaultClasspath,
      "--files",
      "IgnoreMe.scala",
      "--files",
      removeImportsPath.toString()
    ),
    preprocess = { root =>
      Files.createFile(root.resolve("IgnoreMe.scala").toNIO)
    },
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "-P:semanticdb:exclude",
    args = Array(
      s"--scalacOptions",
      s"-P:semanticdb:exclude:IgnoreMe.scala",
      "--classpath",
      defaultClasspath,
      "--files",
      "IgnoreMe.scala",
      "--files",
      removeImportsPath.toString()
    ),
    preprocess = { root =>
      Files.createFile(root.resolve("IgnoreMe.scala").toNIO)
    },
    expectedExit = ExitStatus.Ok
  )

  test("ondemand") {
    val root = StringFS.fromString(
      """
        |/NoSemanticdb.scala
        |package a
        |object NoSemanticdb {
        |  def foo = Option.empty[Int]
        |}
        |/.scalafix.conf
        |rules = ExplicitResultTypes
        |ExplicitResultTypes.memberKind = [Val, Def, Var]
        |ExplicitResultTypes.memberVisibility = [Public, Protected]
        |""".stripMargin
    )
    val (_, exit) = runMain(
      Array(
        "--files",
        "NoSemanticdb.scala",
        "--classpath",
        defaultClasspath
      ),
      root.toNIO
    )
    assert(exit.is(ExitStatus.MissingSemanticdbError))
  }

  checkSemantic(
    name = "ScalaVersion Scala3",
    args = Array(
      "--classpath",
      defaultClasspath,
      "--scalaVersion",
      "3.0.0-RC3"
    ),
    expectedExit = ExitStatus.Ok
  )

}
