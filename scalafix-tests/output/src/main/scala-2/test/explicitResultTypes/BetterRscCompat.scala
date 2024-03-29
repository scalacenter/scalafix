
package test.explicitResultTypes

import BetterRscCompat_Test.AutoImport.oa.ob.od
import testpkg._
import scala.collection.mutable

object BetterRscCompat_Test {

  object ShortenInferredType {

    import scala.collection.mutable

    def int = 42
    def map: mutable.Map[Int,String] = mutable.Map(1 -> "1")
    def str = "hello"
    def str2: String = s"$str world"
    val foo: Int => (Int => Int) = (x: Int) => (y: Int) => x + y
    val bar: (Int => Int) => (Int => Int) = (f: Int => Int) => f
    val baz: Int => (() => (Int => Int) => (Int => Int)) = (f: Int) => bar _
    def qux[A]: (A, A => A) => A = (x: A, y: A => A) => x


    class MyClass

    val obj: MyClass = new MyClass
  }

  object InferInitTypeArgs {

    class CA[A]

    class CB[A](x: A)

    class CC[A](x: Int)

    class CD extends CA
    object CD extends CA

    class CE extends CB
    object CE extends CB

    class CF extends CC(1)
    object CF extends CC(1)

    class CG extends CB(42)
    object CG extends CB(42)

    class CH extends CB[Int](43)
    object CH extends CB[Int](43)

    class CI extends CG
    object CI extends CG

    object nested {
      class CA3[A, B, C](x: A, y: B, z: C)
    }

    class CB3[B](b: B) extends nested.CA3(new CA, b, "z")

    class CC3[B](b: B) extends nested.CA3[Int, Int, Int](1, 2, 3)
  }

  object AutoImport {

    object oa {
      class MyClass
      object ob {
        object od {
          class MyClass
        }
      }
      object oc {
        object od {
          class MyClass

          import scala.collection.{mutable => mut}

          val map: mut.Map[MyClass,oe.MyClass2] = mut.Map(new MyClass -> new oe.MyClass2)
        }
      }
      object oe {
        class MyClass2 {
          class Inner
        }
      }
      object of {
        class MyClass2

        val mc2: MyClass2 = new MyClass2
      }
    }

    val x: oa.MyClass = new oa.MyClass

    val y: od.MyClass = new od.MyClass

    val z: mutable.Map[oa.oc.od.MyClass,oa.oe.MyClass2] = oa.oc.od.map

    private object po {
      val v1: oa.oe.MyClass2 = new oa.oe.MyClass2
      val w1: v1.Inner = new v1.Inner
    }
    val v2: oa.oe.MyClass2 = new oa.oe.MyClass2
    val w2: v2.Inner = new v2.Inner
  }

  object ClassLinearization {

    trait MyTrait1 {
      class MyInnerClass1

      def foo: MyInnerClass1 = new MyInnerClass1
    }

    trait MyTrait2 extends MyTrait1

    object myobj extends MyTrait2 {
      def bar: MyInnerClass1 = foo
    }

    val baz: myobj.MyInnerClass1 = myobj.bar

    object myError extends Exception {
      def foo = 1
    }
  }

  object AnyrefWithTrait {

    trait Trait1 {
      def foo: Int
    }
    trait Trait2 {
      val bar: Int
    }

    val t1: Trait1 = new Trait1 {
      def foo: Int = 1
    }

    val t2: Trait1 with Trait2 = new Trait1 with Trait2 {
      def foo: Int = 1
      val bar: Int = 1
    }

    val t3: t3 = new t3
    class t3 extends Trait2 {
      def foo: Int = 1
      val bar: Int = 1
    }

    val tSeq: Seq[Trait1] = Seq(new Trait1 { def foo = 1 })

    val anonObj: AnyRef = new {}

    val anonObj2: Trait2 = new {} with Trait2 {
      val bar = 2
    }

    val anonObj3: Trait2 = new { val bar: Int = 3 } with Trait2

    def order[T]: Ordering[T] = new Ordering[T] {
      override def compare(x: T, y: T): Int = 0
    }
  }

  object TypeAliases {

    object scope1 {
      class MyClass

      def foo: MyClass = null
    }

    object scope2 {
      type MyClass = scope1.MyClass

      def foo: MyClass = null
    }

    object right2left {
      import scope1.MyClass
      val bar: MyClass = scope2.foo
    }

    object left2right {
      import scope2.MyClass
      val bar: MyClass = scope1.foo
    }
  }

  object PackageObjects {
    val o2c: O2.C2 = O1.foo
  }

  object PolymorphicDefaultParams {

    def foo[A](x: A = null): A = x

    def opt[A](x: Option[A] = None): A = x.get

    def both[A, B](x: A = null)(y: Option[B] = None): Option[(B, A)] = y.map((_, x))

    def outer[A](x: A = null): (A, A, A) = {
      def inner[B](y: A = x, z: B = null) = (x, y, z)
      inner(x, x)
    }

    class Z
    class Y extends Z

    abstract class X[A >: Y](val x1: A = null, val x2: Option[A] = None) {

      def this(x4: A) = this(x4, Some(x4))

      def bar[B <: Z](a: Int, b: B, a2: Int = 4, c: A = new Y)
        (d: A)
        (e: B = new Y, f: Option[B] = {val x = new Z; Some(x)}): (Option[B], A) = (f, c)

      def bar2[B >: Y](a: B, b: B, c: A, e: B)

      def bar2[B >: Y](a: Int, b: B, c: A = new Y, e: B = new Y)

      class Inner[B](val x1: A = null, val x2: B = null)
        (x3: B = null, x4: B = null)

      class Inner2[B](val a: A, val b: B, val c: B) extends Inner(a, a)(a, a) {
        def this(b: B) = this(null, b, b)

        def this(a1: A = null, b1: B = 5) = this(a1, b1, b1)
      }

      class ExtendSecondaryCtor[B](x: B = null)(val y: B = null) extends Inner2(null, x)

      class PrivateCtor[B] private(x: B = null)
    }

    abstract class Ascribed[A >: Y](val x1: A = null: Null, val x2: Option[A] = None) {

      def this(x4: A) = this(x4, Some(x4))

      def bar[B <: Z](a: Int, b: B, a2: Int = 4: Int, c: A = new Y: Y)
        (d: A)
        (e: B = new Y: Y, f: Option[B] = {val x = new Z; Some(x)}: Option[Z]): (Option[B], A) = (f, c)

      def bar2[B >: Y](a: Int, b: B, c: A = new Y, e: B = new Y: Y)

      class Inner[B](val x1: A = null: Null, val x2: B = null: Null)
        (x3: B = null: Null, x4: B = null)

      class Inner2[B](val a: A, val b: B, val c: B) extends Inner(a, b)(b, b) {
        def this(a1: A = null: Null, b1: B = 5) = this(a1, b1, b1)
      }
    }
  }

  object NestedTuples {
    val foo: ((String, Int), Double) = (("1", 1), 1.0)
  }

  object PolymorphicGrandparent {
    class C[A]

    abstract class D[A]

    trait E[A]

    trait T extends C[Int]

    trait U extends D[Int]

    trait V extends E[Int]

    class F extends T

    class G extends U

    class H extends V
  }
}
