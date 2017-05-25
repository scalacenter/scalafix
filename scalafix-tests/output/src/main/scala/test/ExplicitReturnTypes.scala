package test

object ExplicitReturnTypesInput {
  implicit val L: List[Int] = List(1)
  implicit val M: scala.collection.immutable.Map[Int, String] = Map(1 -> "STRING")
  implicit def D: Int = 2
  implicit def tparam[T](e: T): T = e
  implicit def tparam2[T](e: T): List[T] = List(e)
  implicit def tparam3[T](e: T): scala.collection.immutable.Map[T, T] = Map(e -> e)
  class Path {
    class B { class C }
    implicit val x: Path.this.B = new B
    implicit val y: Path.this.x.C = new x.C
  }
  class TwoClasses[T](e: T)
  class TwoClasses2 {
    implicit val x: test.ExplicitReturnTypesInput.TwoClasses[Int] = new TwoClasses(10)
  }
  class implicitlytrick {
    implicit val s: String = "string"
    implicit val x = implicitly[String]
  }
  object InnerInnerObject {
    object B {
      class C
      object C {
        implicit val x: List[test.ExplicitReturnTypesInput.InnerInnerObject.B.C] = List(new C)
      }
    }
  }
  object SiblingObject {
    class B
  }
  object A {
    class C {
      implicit val x: List[test.ExplicitReturnTypesInput.SiblingObject.B] = List(new SiblingObject.B)
    }
  }
  object slick {
    case class Supplier(id: Int, name: String)
    implicit val supplierGetter: ((Int, String)) => test.ExplicitReturnTypesInput.slick.Supplier = (arg: (Int, String)) =>
      Supplier(arg._1, arg._2)
  }
}

class DeeperPackage[T](e: T)
package foo {
  class B {
    implicit val x: test.DeeperPackage[Int] = new DeeperPackage(10)
  }
}

package shallwpackage {
  class A[T](e: T)
}
class shallobpackage {
  implicit val x: test.shallwpackage.A[Int] = new shallwpackage.A(10)
}

package enclosingPackageStripIsLast { class B }
package a {
  import enclosingPackageStripIsLast.B
  class A {
    implicit val x: test.enclosingPackageStripIsLast.B = new B
  }
}
