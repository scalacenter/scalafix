package test

import scala.language.implicitConversions

object ExplicitReturnTypes {
  val a: scala.Int = 1 + 2
  def b(): java.lang.String = "a" + "b"
  var c: scala.Boolean = 1 == 1
  protected val d = 1.0f
  protected def e(a:Int,b:Double): scala.Double = a + b
  protected var f: scala.Function1[scala.Int, scala.Int] = (x: Int) => x + 1
  private val g = 1
  private def h(a:Int) = ""
  private var i = 1

  private implicit var j: scala.Int = 1

  implicit val L: scala.collection.immutable.List[scala.Int] = List(1)
  implicit val M: scala.collection.immutable.Map[scala.Int, java.lang.String] = Map(1 -> "STRING")
  implicit def D: scala.Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): scala.collection.immutable.List[T] = List(e)
  implicit def tparam3[T](e: T): scala.collection.immutable.Map[T, T] = Map(e -> e)
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: Path.this.x.C = new x.C
  }
  class TwoClasses[T](e: T)
  class TwoClasses2 {
    implicit val x: test.ExplicitReturnTypes.TwoClasses[scala.Int] = new TwoClasses(10)
  }
  class implicitlytrick {
    implicit val s: java.lang.String = "string"
    implicit val x = implicitly[String]
  }
  object InnerInnerObject {
    object B {
      class C
      object C {
        implicit val x: scala.collection.immutable.List[test.ExplicitReturnTypes.InnerInnerObject.B.C] = List(new C)
      }
    }
  }
  object SiblingObject {
    class B
  }
  object A {
    class C {
      implicit val x: scala.collection.immutable.List[test.ExplicitReturnTypes.SiblingObject.B] = List(new SiblingObject.B)
    }
  }
  object slick {
    case class Supplier(id: Int, name: String)
    implicit val supplierGetter: ((scala.Int, scala.Predef.String)) => test.ExplicitReturnTypes.slick.Supplier = (arg: (Int, String)) =>
      Supplier(arg._1, arg._2)
  }
  def foo(x: Int): scala.Int =
    // comment
    x + 2
  object ExtraSpace {
    def * : scala.Int = "abc".length
    def foo_ : scala.Int = "abc".length
    def `x`: scala.Int = "abc".length
    def `x `: scala.Int = "abc".length
  }
}

class DeeperPackage[T](e: T)
package foo {
  class B {
    implicit val x: test.DeeperPackage[scala.Int] = new DeeperPackage(10)
  }
}

package shallwpackage {
  class A[T](e: T)
}
class shallobpackage {
  implicit val x: test.shallwpackage.A[scala.Int] = new shallwpackage.A(10)
}

package enclosingPackageStripIsLast { class B }
package a {
  import enclosingPackageStripIsLast.B
  class A {
    implicit val x: test.enclosingPackageStripIsLast.B = new B
  }
}
