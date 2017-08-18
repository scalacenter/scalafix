package scalafix.cli

import scala.collection.immutable.Seq
import scalafix.tests.BuildInfo

class CliSemanticTest extends BaseCliTest {

  checkSemantic(
    name = "--classpath inference error",
    args = Nil,
    expectedExit = ExitStatus.InvalidCommandLineOption,
    fileIsFixed = false,
    outputAssert = output => {
      assert(output.contains("Unable to infer --classpath"))
    }
  )

  checkSemantic(
    name = "--classpath explicit is OK",
    args = Seq(
      "--classpath",
      BuildInfo.semanticClasspath.getAbsolutePath
    ),
    expectedExit = ExitStatus.Ok,
    fileIsFixed = true
  )
}
