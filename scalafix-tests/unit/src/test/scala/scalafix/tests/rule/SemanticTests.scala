package scalafix.tests.rule

import scala.meta._
import scala.meta.internal.io.PathIO
import scala.meta.semanticdb.SemanticdbSbt
import scalafix.SemanticdbIndex
import scalafix.testkit._
import scalafix.tests.BuildInfo
import scalafix.tests.rule.SemanticTests._

class SemanticTests
    extends SemanticRuleSuite(
      index,
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputSbtSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}

object SemanticTests {
  def index: SemanticdbIndex = SemanticdbIndex.load(
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
      // TODO: Figure out whether this is necessary.
      // AbsolutePath(BuildInfo.semanticSbtClasspath),
      AbsolutePath(BuildInfo.semanticClasspath)
    )
  )

}
