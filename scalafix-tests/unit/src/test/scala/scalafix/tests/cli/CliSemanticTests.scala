package scalafix.tests.cli

import scala.meta.internal.io.PathIO
import scala.collection.immutable.Seq
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
    expectedExit = ExitStatus.InvalidCommandLineOption,
    outputAssert = msg => {
      assert(msg.contains("--sourceroot"))
      assert(msg.contains("bogus"))
    }
  )

  checkSemantic(
    name = "MissingSemanticDB",
    args = Nil, // no --classpath
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
