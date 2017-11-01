package scalafix.testkit

import scala.language.experimental.macros

import scala.reflect.ClassTag
import utest._
import utest.asserts.Asserts
import utest.framework._

trait ScalafixTest extends TestSuite {
  class TestFailedException(msg: String) extends Exception(msg)
  def beforeAll(): Unit = ()
  def afterAll(): Unit = ()
  def intercept[T: ClassTag](exprs: Unit): T = macro Asserts.interceptProxy[T]
  def assert(exprs: Boolean*): Unit = macro Asserts.assertProxy
  def assertNoDiff(
      obtained: String,
      expected: String,
      title: String = ""
  ): Boolean = DiffAssertions.assertNoDiff(obtained, expected, title)
  override def utestAfterAll(): Unit = afterAll()
  private val myTests = IndexedSeq.newBuilder[(String, () => Unit)]
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
