package scalafix
package tests

import scala.meta._
import scala.meta.sbthost.Sbthost
import scalafix.testkit._
import lang.meta.internal.io.PathIO
import SemanticTests._

class SemanticTests
    extends SemanticRewriteSuite(
      sctx,
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputSbtSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}

object SemanticTests {
  def sctx = SemanticCtx.load(
    Sbthost.patchDatabase(
      Database.load(
        classpath,
        sourcepath
      ),
      PathIO.workingDirectory
    ),
    sourcepath,
    classpath
  )
  def sourcepath: Sourcepath = Sourcepath(
    List(
      AbsolutePath(BuildInfo.inputSourceroot),
      AbsolutePath(BuildInfo.inputSbtSourceroot)
    )
  )
  def classpath: Classpath = Classpath(
    List(
      AbsolutePath(BuildInfo.semanticSbtClasspath),
      AbsolutePath(BuildInfo.semanticClasspath)
    )
  )

}
