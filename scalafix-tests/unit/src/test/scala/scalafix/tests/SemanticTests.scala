package scalafix
package tests

import scala.meta._
import scala.meta.sbthost.Sbthost
import scalafix.testkit._
import lang.meta.internal.io.PathIO

class SemanticTests
    extends SemanticRewriteSuite(
      SemanticCtx.load(
        Sbthost.patchDatabase(
          Database.load(
            Classpath(
              List(
                AbsolutePath(BuildInfo.semanticSbtClasspath),
                AbsolutePath(BuildInfo.semanticClasspath)
              )
            )
          ),
          PathIO.workingDirectory
        )
      ),
      List(
        AbsolutePath(BuildInfo.inputSourceroot),
        AbsolutePath(BuildInfo.inputSbtSourceroot)
      ),
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputSbtSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}
