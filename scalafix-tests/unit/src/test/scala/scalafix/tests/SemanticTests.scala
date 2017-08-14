package scalafix
package tests

import scala.meta._
import scala.meta.sbthost.Sbthost
import scalafix.testkit._
import lang.meta.internal.io.PathIO

class SemanticTests
    extends SemanticRewriteSuite(
      SemanticTests.defaultCtx,
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

object SemanticTests {
  def defaultCtx: SemanticCtx = {
    val impl = Sbthost.patchDatabase(
      Database.load(Classpath(AbsolutePath(BuildInfo.semanticSbtClasspath))),
      PathIO.workingDirectory)
    SemanticCtx
      .load(impl)
      .merge(
        SemanticCtx.load(Classpath(AbsolutePath(BuildInfo.semanticClasspath)))
      )
  }
}
