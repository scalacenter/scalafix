package scalafix.testkit.utest

import scala.language.experimental.macros

import scala.reflect.ClassTag
import scalafix.testkit
import _root_.utest._
import _root_.utest.asserts.Asserts
import _root_.utest.framework._
import org.scalameta.FileLine

abstract class ScalafixTest extends TestSuite with testkit.BaseScalafixSuite {
  def beforeAll(): Unit = ()
  def afterAll(): Unit = ()
  def intercept[T: ClassTag](exprs: Unit): T = macro Asserts.interceptProxy[T]
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  override def utestAfterAll(): Unit = afterAll()
  private val myTests = IndexedSeq.newBuilder[(String, () => Unit)]

  override def scalafixTest(name: String)(fun: => Any)(
      implicit pos: FileLine): Unit =
    this.test(name)(fun)
  def test(name: String)(fun: => Any): Unit = {
    myTests += (name -> (() => fun))
  }
  final override def tests: Tests = {
    val ts = myTests.result()
    val names = Tree("", ts.map(x => Tree(x._1)): _*)
    val thunks = new TestCallTree({
      beforeAll()
      Right(ts.map(x => new TestCallTree(Left(x._2()))))
    })
    Tests.apply(names, thunks)
  }
}
