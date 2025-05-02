
package test.explicitResultTypes

import scala.language.implicitConversions
import scala.concurrent.Future

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
  val k: (Int, String) = (1, "msg")
  def comment(x: Int): Int =
    // comment
    x + 2
  object ExtraSpace {
    def * : Int = "abc".length
    def ! : Int = "abc".length
    def `x`: Int = "abc".length
    def `x `: Int = "abc".length
  }
  locally {
    implicit val Implicit: Future[Int] = scala.concurrent.Future.successful(2)
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
