package scalafix
package tests

import scala.meta._
import scala.meta.semanticdb.SemanticdbSbt
import scalafix.testkit._
import scala.meta.internal.io.PathIO
import SemanticTests._

class SemanticTests
    extends SemanticRuleSuite(
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
  def sctx: SemanticdbIndex = SemanticdbIndex.load(
    SemanticdbSbt.patchDatabase(
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
