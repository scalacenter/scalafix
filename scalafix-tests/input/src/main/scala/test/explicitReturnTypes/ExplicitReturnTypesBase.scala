/*
rewrites = ExplicitReturnTypes
explicitReturnTypes.memberKind = [Val, Def, Var]
explicitReturnTypes.memberVisibility = [Public, Protected]
 */
package test.explicitReturnTypes

import scala.language.implicitConversions

object ExplicitReturnTypesBase {
  def none[T] =  None.asInstanceOf[Option[T]]
  val a = 1 + 2
  def b() = "a" + "b"
  var c = 1 == 1
  protected val d = 1.0f
  protected def e(a: Int, b: Double) = a + b
  protected var f = (x: Int) => x + 1
  private val g = 1
  private def h(a: Int) = ""
  private var i = 22

  locally {
    implicit val Implicit = scala.concurrent.Future.successful(2)
    val Var = scala.concurrent.Future.successful(2)
    val Val = scala.concurrent.Future.successful(2)
    def Def = scala.concurrent.Future.successful(2)
  }

  private implicit var j = 1

  implicit val L = List(1)
  implicit val M = Map(1 -> "STRING")
  implicit def D = 2
  implicit def tparam[T](e: T) = e
  implicit def tparam2[T](e: T) = List(e)
  implicit def tparam3[T](e: T) = Map(e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
  def comment(x: Int) =
    // comment
    x + 2
  object ExtraSpace {
    def * = "abc".length
    def foo_ = "abc".length
    def `x` = "abc".length
    def `x ` = "abc".length
  }
}
