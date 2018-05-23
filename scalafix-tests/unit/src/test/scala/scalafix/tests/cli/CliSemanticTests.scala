package scalafix.tests.cli

import org.langmeta.internal.io.PathIO
import scala.collection.immutable.Seq
import scalafix.cli._
import scalafix.internal.cli.CommonOptions
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
      "--cwd",
      PathIO.workingDirectory.toString(),
      "--auto-classpath"
    ),
    expectedExit = ExitStatus.Ok
  )

  checkSemantic(
    name = "--sourceroot is not a file",
    args = Seq(
      "--sourceroot",
      "bogus",
      "--classpath",
      semanticClasspath
    ),
    expectedExit = ExitStatus.InvalidCommandLineOption,
    outputAssert = msg => assert(msg.contains("Invalid --sourceroot"))
  )

  checkSemantic(
    name = "--sourceroot points to non-existing file",
    args = Seq(
      "--sourceroot",
      CommonOptions.default.workingDirectory,
      "--classpath",
      semanticClasspath
    ),
    expectedExit = ExitStatus.InvalidCommandLineOption,
    outputAssert = msg => {
      assert(msg.contains("is not a file"))
      assert(msg.contains("Is --sourceroot correct?"))
    }
  )

  checkSemantic(
    name = "wrong --sourceroot results in MissingSemanticDB",
    args = Seq(
      "--sourceroot",
      PathIO.workingDirectory
        .resolve("scalafix-tests")
        .resolve("input")
        .resolve("src")
        .resolve("main")
        .resolve("scala")
        .toString(),
      "--classpath",
      semanticClasspath
    ),
    files = s"**/${removeImportsPath.toNIO.getFileName.toString}",
    expectedExit = ExitStatus.MissingSemanticDB,
    outputAssert = msg => {
      assert(msg.contains("No SemanticDB associated with"))
      assert(msg.contains(removeImportsPath.toNIO.getFileName.toString))
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
