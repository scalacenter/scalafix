package scalafix.testkit.scalatest

import scala.meta.AbsolutePath
import scala.meta.Classpath
import scala.meta.Database
import scala.meta.Sourcepath
import scalafix.testkit
import org.scalactic.source.Position
import org.scalameta.FileLine
import org.scalatest.FunSuiteLike

trait ScalafixSuite extends FunSuiteLike with testkit.BaseScalafixSuite {
  override def scalafixTest(name: String)(fun: => Any)(
      implicit pos: FileLine): Unit =
    super.test(name)(fun)(
      Position(pos.file.value, pos.file.value, pos.line.value))
}
