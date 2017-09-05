package test

import scala.language.implicitConversions

object ExplicitReturnTypes {
  def none[T]: _root_.scala.Option[T] =  None.asInstanceOf[Option[T]]
  val a: _root_.scala.Int = 1 + 2
  def b(): _root_.java.lang.String = "a" + "b"
  var c: _root_.scala.Boolean = 1 == 1
  protected val d = 1.0f
  protected def e(a: Int, b: Double): _root_.scala.Double = a + b
  protected var f: _root_.scala.Int => _root_.scala.Int = (x: Int) => x + 1
  private val g = 1
  private def h(a: Int) = ""
  private var i = 22

  locally {
    implicit val Implicit: _root_.scala.concurrent.Future[_root_.scala.Int] = scala.concurrent.Future.successful(2)
    val Var = scala.concurrent.Future.successful(2)
    val Val = scala.concurrent.Future.successful(2)
    def Def = scala.concurrent.Future.successful(2)
  }

  private implicit var j: _root_.scala.Int = 1

  implicit val L: _root_.scala.collection.immutable.List[_root_.scala.Int] = List(1)
  implicit val M: _root_.scala.collection.immutable.Map[_root_.scala.Int, _root_.java.lang.String] = Map(1 -> "STRING")
  implicit def D: _root_.scala.Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): _root_.scala.collection.immutable.List[T] = List(e)
  implicit def tparam3[T](e: T): _root_.scala.collection.immutable.Map[T, T] = Map(e -> e)
  class TwoClasses[T](e: T)
  class TwoClasses2 {
    implicit val x: _root_.test.ExplicitReturnTypes.TwoClasses[_root_.scala.Int] = new TwoClasses(10)
  }
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
  object InnerInnerObject {
    object B {
      class C
      object C {
        implicit val x: _root_.scala.collection.immutable.List[_root_.test.ExplicitReturnTypes.InnerInnerObject.B.C] = List(new C)
      }
    }
  }
  object SiblingObject {
    class B
  }
  object A {
    class C {
      implicit val x: _root_.scala.collection.immutable.List[_root_.test.ExplicitReturnTypes.SiblingObject.B] = List(new SiblingObject.B)
    }
  }
  object slick {
    case class Supplier(id: Int, name: String)
    implicit val supplierGetter: ((_root_.scala.Int, _root_.scala.Predef.String)) => _root_.test.ExplicitReturnTypes.slick.Supplier = (arg: (Int, String)) =>
      Supplier(arg._1, arg._2)
  }
  def foo(x: Int): _root_.scala.Int =
    // comment
    x + 2
  object ExtraSpace {
    def * : _root_.scala.Int = "abc".length
    def foo_ : _root_.scala.Int = "abc".length
    def `x`: _root_.scala.Int = "abc".length
    def `x `: _root_.scala.Int = "abc".length
  }
}

class DeeperPackage[T](e: T)
package foo {
  class B {
    implicit val x: _root_.test.DeeperPackage[_root_.scala.Int] = new DeeperPackage(10)
  }
}

package shallwpackage {
  class A[T](e: T)
}
class shallobpackage {
  implicit val x: _root_.test.shallwpackage.A[_root_.scala.Int] = new shallwpackage.A(10)
}

package enclosingPackageStripIsLast { class B }
package a {
  import enclosingPackageStripIsLast.B
  class A {
    implicit val x: _root_.test.enclosingPackageStripIsLast.B = new B
  }
}
