package scalafix.tests.rule

import scala.meta._
import scalafix.SemanticdbIndex
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import scalafix.testkit._
import scalafix.tests.BuildInfo
import scalafix.tests.rule.SemanticTests._
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.LazySymbolTable

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
  def defaultClasspath = Classpath(
    classpath.shallow ++
      Classpath(RuleCompiler.defaultClasspath).shallow
        .filter(_.toString().contains("scala-library"))
  )
  def index: SemanticdbIndex = EagerInMemorySemanticdbIndex(
    Database.load(
      classpath,
      sourcepath
    ),
    sourcepath,
    classpath,
    new LazySymbolTable(
      ClasspathOps.toMetaClasspath(defaultClasspath, System.out))
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
