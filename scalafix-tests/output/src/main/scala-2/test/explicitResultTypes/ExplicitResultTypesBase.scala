
package test.explicitResultTypes

import scala.language.implicitConversions

object ExplicitResultTypesBase {
  def none[T]: Option[T] =  None.asInstanceOf[Option[T]]
  val a: Int = 1 + 2
  def b(): String = "a" + "b"
  var c: Boolean = 1 == 1
  protected val d = 1.0f
  protected def e(a: Int, b: Double): Double = a + b
  protected var f: Int => Int = (x: Int) => x + 1
  val f0: () => Int = () => 42
  private val g = 1
  private def h(a: Int) = ""
  private var i = 22
  private implicit var j = 1
  val k: (Int, String) = (1, "msg")
  implicit val L: List[Int] = List(1)
  implicit val M: Map[Int,String] = Map(1 -> "STRING")
  implicit def D: Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): List[T] = List(e)
  implicit def tparam3[T](e: T): Map[T,T] = Map(e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
  def comment(x: Int): Int =
    // comment
    x + 2
  object ExtraSpace {
    def * = "abc".length
    def foo_ : Int = "abc".length
    def `x`: Int = "abc".length
    def `x ` = "abc".length
  }
  locally {
    implicit val Implicit = scala.concurrent.Future.successful(2)
    val Var = scala.concurrent.Future.successful(2)
    val Val = scala.concurrent.Future.successful(2)
    def Def = scala.concurrent.Future.successful(2)
  }
  object unicode {
    object `->` {
      def unapply[S](in: (S, S)): Option[(S, S)] = Some(in)
    }
    val `→` = `->`
  }
  def tuple: ((Int, String)) => String = null.asInstanceOf[((Int, String)) => String]
}
