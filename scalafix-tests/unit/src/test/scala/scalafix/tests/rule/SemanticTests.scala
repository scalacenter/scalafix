package scalafix.tests.rule

import scala.meta._
import scalafix.SemanticdbIndex
import scalafix.testkit._
import scalafix.tests.BuildInfo
import scalafix.tests.rule.SemanticTests._

class SemanticTests
    extends SemanticRuleSuite(
      index,
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}

object SemanticTests {
  def index: SemanticdbIndex = SemanticdbIndex.load(
    Database.load(
      classpath,
      sourcepath
    ),
    sourcepath,
    classpath
  )
  def sourcepath: Sourcepath = Sourcepath(
    List(
      AbsolutePath(BuildInfo.inputSourceroot)
    )
  )
  def classpath: Classpath = Classpath(
    List(
      AbsolutePath(BuildInfo.semanticClasspath)
    )
  )

}
