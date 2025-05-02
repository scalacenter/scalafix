/*
rules = ExplicitResultTypes
ExplicitResultTypes.memberKind = [Val, Def, Var]
ExplicitResultTypes.memberVisibility = [Public, Protected]
 */
package test.explicitResultTypes

import scala.language.implicitConversions

object ExplicitResultTypesBase {
  def none[T] =  None.asInstanceOf[Option[T]]
  val a = 1 + 2
  def b() = "a" + "b"
  var c = 1 == 1
  protected val d = 1.0f
  protected def e(a: Int, b: Double) = a + b
  protected var f = (x: Int) => x + 1
  val f0 = () => 42
  private val g = 1
  private def h(a: Int) = ""
  private var i = 22
  val k = (1, "msg")
  def comment(x: Int) =
    // comment
    x + 2
  object ExtraSpace {
    def * = "abc".length
    def ! = "abc".length
    def `x` = "abc".length
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
  def tuple = null.asInstanceOf[((Int, String)) => String]
}
