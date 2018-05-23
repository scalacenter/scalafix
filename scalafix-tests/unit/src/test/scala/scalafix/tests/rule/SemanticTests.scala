package scalafix.tests.rule

import scala.meta._
import scalafix.SemanticdbIndex
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.EagerInMemorySemanticdbIndex
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
    classpath.shallow ++
      RuleCompiler.defaultClasspathPaths.filter(path =>
        path.toNIO.getFileName.toString.contains("scala-library"))
  )
  def index: SemanticdbIndex = EagerInMemorySemanticdbIndex(
    Database.load(
      classpath,
      sourcepath
    ),
    sourcepath,
    classpath,
    ClasspathOps.newSymbolTable(defaultClasspath).getOrElse {
      sys.error("Failed to load symbol table")
    }
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
