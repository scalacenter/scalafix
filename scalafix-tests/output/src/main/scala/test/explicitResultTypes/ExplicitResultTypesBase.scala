package test.explicitResultTypes

import scala.language.implicitConversions

object ExplicitResultTypesBase {
  def none[T]: _root_.scala.Option[T] =  None.asInstanceOf[Option[T]]
  val a: _root_.scala.Int = 1 + 2
  def b(): _root_.java.lang.String = "a" + "b"
  var c: _root_.scala.Boolean = 1 == 1
  protected val d = 1.0f
  protected def e(a: Int, b: Double): _root_.scala.Double = a + b
  protected var f: _root_.scala.Int => _root_.scala.Int = (x: Int) => x + 1
  val f0: () => _root_.scala.Int = () => 42
  private val g = 1
  private def h(a: Int) = ""
  private var i = 22
  private implicit var j: _root_.scala.Int = 1
  val k: (_root_.scala.Int, _root_.java.lang.String) = (1, "msg")
  implicit val L: _root_.scala.collection.immutable.List[_root_.scala.Int] = List(1)
  implicit val M: _root_.scala.collection.immutable.Map[_root_.scala.Int, _root_.java.lang.String] = Map(1 -> "STRING")
  implicit def D: _root_.scala.Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): _root_.scala.collection.immutable.List[T] = List(e)
  implicit def tparam3[T](e: T): _root_.scala.collection.immutable.Map[T, T] = Map(e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
  def comment(x: Int): _root_.scala.Int =
    // comment
    x + 2
  object ExtraSpace {
    def * : _root_.scala.Int = "abc".length
    def foo_ : _root_.scala.Int = "abc".length
    def `x`: _root_.scala.Int = "abc".length
    def `x `: _root_.scala.Int = "abc".length
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
    val `→`: _root_.test.explicitResultTypes.ExplicitResultTypesBase.unicode.->.type = `->`
  }
}

