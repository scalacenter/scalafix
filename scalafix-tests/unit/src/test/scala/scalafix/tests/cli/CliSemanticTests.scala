package scalafix.tests.cli

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.internal.io.PathIO
import scala.collection.immutable.Seq
import scala.meta.internal.io.FileIO
import scalafix.cli._
import scalafix.internal.rule.ExplicitResultTypes
import scalafix.tests.rule.SemanticTests

class CliSemanticTests extends BaseCliTest {

  checkSemantic(
    name = "--classpath ok",
    args = Seq(
      "--classpath",
      semanticClasspath
    ),
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "--auto-classpath ok",
    args = List(
      "--auto-classpath-roots",
      PathIO.workingDirectory.toString(),
      "--auto-classpath"
    ),
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "--sourceroot not ok",
    args = Seq(
      "--sourceroot",
      "bogus",
      "--classpath",
      semanticClasspath
    ),
    expectedExit = ExitStatus.CommandLineError,
    outputAssert = { out =>
      assert(out.contains("--sourceroot"))
      assert(out.contains("bogus"))
    }
  )

  checkSemantic(
    name = "MissingSemanticDB",
    args = Nil, // no --classpath
    expectedExit = ExitStatus.MissingSemanticdbError,
    outputAssert = { out =>
      assert(out.contains("No SemanticDB associated with"))
      assert(out.contains(removeImportsPath.toNIO.getFileName.toString))
    }
  )

  checkSemantic(
    name = "StaleSemanticDB",
    args = Seq(
      "--classpath",
      SemanticTests.defaultClasspath.syntax
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
      assert(out.contains(explicitResultTypesPath.toString + "-ondisk"))
      assert(out.contains("-// comment"))
    }
  )

  checkSemantic(
    name = "StaleSemanticDB fix matches input",
    args = Seq(
      "--classpath",
      SemanticTests.defaultClasspath.syntax
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
    args = Seq(
      "--classpath",
      SemanticTests.defaultClasspath.syntax
    ),
    expectedExit = ExitStatus.Ok,
    rule = ExplicitResultTypes.toString(),
    path = explicitResultTypesPath,
    files = explicitResultTypesPath.toString()
  )

  checkSemantic(
    name = "incomplete classpath does not result in error exit code",
    args = Seq(
      "--classpath",
      semanticClasspath // missing scala-library
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
