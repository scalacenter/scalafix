package scalafix.tests.rule

import scala.meta._
import scalafix.testkit._
import scalafix.tests.BuildInfo
import scalafix.tests.rule.SemanticTests._
import scalafix.internal.reflect.RuleCompiler

class SemanticTests
    extends SemanticRuleSuite(
      AbsolutePath(BuildInfo.inputSourceroot),
      defaultClasspath,
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}

object SemanticTests {
  def defaultClasspath = Classpath(
    classpath.entries ++
      RuleCompiler.defaultClasspathPaths.filter(path =>
        path.toNIO.getFileName.toString.contains("scala-library"))
  )
  def classpath: Classpath = Classpath(
    List(
      AbsolutePath(BuildInfo.semanticClasspath)
    )
  )

}
