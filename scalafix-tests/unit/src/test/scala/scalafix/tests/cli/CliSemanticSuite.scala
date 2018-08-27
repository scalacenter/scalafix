package scalafix.tests.cli

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.internal.io.PathIO
import scala.meta.internal.io.FileIO
import scala.meta.io.Classpath
import scalafix.cli._
import scalafix.internal.rule.ExplicitResultTypes

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
        "RemoveUnusedImports",
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
    rule = ExplicitResultTypes.toString(),
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString(),
    outputAssert = { out =>
      assert(out.contains("Stale SemanticDB"))
      assert(
        out.contains(
          explicitResultTypesPath.toString + "-ondisk-md5-fingerprint"))
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
    rule = ExplicitResultTypes.toString(),
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
    rule = ExplicitResultTypes.toString(),
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString()
  )

  checkSemantic(
    name = "incomplete classpath does not result in error exit code",
    args = Array(
      "--classpath",
      Classpath(
        props.inputClasspath.entries
          .filterNot(_.toString().contains("scala-library"))).syntax
    ),
    // Errors in ExplicitResultTypes are suppressed.
    expectedExit = ExitStatus.Ok,
    rule = ExplicitResultTypes.toString(),
    path = explicitResultTypesPath,
    assertObtained = { _ =>
      // Do nothing
    }
  )

}
