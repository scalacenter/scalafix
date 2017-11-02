package scalafix.tests

import scala.meta.AbsolutePath
import scala.meta.Classpath
import scala.meta.Database
import scala.meta.Sourcepath
import scala.meta.internal.io.PathIO
import scala.meta.semanticdb.SemanticdbSbt
import scalafix.SemanticdbIndex

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
      AbsolutePath(BuildInfo.semanticSbtClasspath),
      AbsolutePath(BuildInfo.semanticClasspath)
    )
  )

}
