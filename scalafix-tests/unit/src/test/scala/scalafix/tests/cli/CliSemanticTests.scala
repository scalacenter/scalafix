package scalafix.tests.cli

import scala.collection.immutable.Seq
import scalafix.cli._
import scalafix.internal.cli.CommonOptions
import scalafix.internal.rule.ExplicitResultTypes
import scalafix.tests.rule.SemanticTests

class CliSemanticTests extends BaseCliTest {

  checkSemantic(
    name = "--classpath inference error",
    args = Nil,
    expectedExit = ExitStatus.InvalidCommandLineOption,
    outputAssert = output => {
      assert(output.contains("Unable to infer --classpath"))
    }
  )

  checkSemantic(
    name = "--classpath explicit is OK",
    args = Seq(
      "--classpath",
      semanticClasspath
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
    name = "--sourceroot does not resolve all --files",
    args = Seq(
      "--sourceroot",
      CommonOptions.default.workingPath
        .resolve("scalafix-tests")
        .resolve("input")
        .resolve("src")
        .resolve("main")
        .toString(),
      "--classpath",
      semanticClasspath
    ),
    expectedExit = ExitStatus.InvalidCommandLineOption,
    outputAssert = msg => {
      assert(msg.contains("No semanticdb associated with"))
      assert(msg.contains(removeImportsPath.toString()))
      assert(msg.contains("Is --sourceroot correct?"))
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
    path = explicitResultTypesPath
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
