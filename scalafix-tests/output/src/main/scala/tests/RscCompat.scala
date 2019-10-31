
package rsc.tests

import com.twitter.bijection._
import java.util.Base64
import scala.language.existentials
import scala.language.higherKinds
import rsc.tests.foo.T

object RscCompat_Test {
  class Basic {
    def x1 = 42
    val x2 = ""
    final val x3 = ""
    var x4 = ""
  }

  class Patterns {
    val List() = List()
    val List(x2) = List(2)
    val List(x3, y3) = List(3, 3)
    val x4, y4 = 4
    val x9 :: y9 = List(9, 9, 9)
    var List() = List()
    var List(x6) = List(6)
    var List(x7, y7) = List(7, 7)
    var x8, y8 = 8
    var x10 :: y10 = List(10, 10, 10)
  }

  class Visibility {
    private def x1 = ""
    private[this] def x2 = ""
    private[rsc] def x3 = ""
    protected def x4 = ""
    protected[this] def x5 = ""
    protected[rsc] def x6 = ""
  }

  private class Private { def x1 = "" }
  private[this] class PrivateThis { def x1 = "" }
  private[rsc] class PrivateRsc { def x1 = "" }
  protected class Protected { def x1 = "" }
  protected[this] class ProtectedThis { def x1 = "" }
  protected[rsc] class ProtectedRsc { def x1 = "" }

  private trait PrivateTrait { def x1 = "" }
  trait PublicTrait {
    private def x1 = ""
    private[this] def x2 = ""
    private[rsc] def x3 = ""
    protected def x4 = ""
    protected[this] def x5 = ""
    protected[rsc] def x6 = ""
  }

  object Config {
    val x = 1
    val y = 2
  }

  object TypesHelpers {
    class C
    class E {
      class C
    }
    class P {
      class C
      val c: C = ???
    }
    val p: P = new P
    val c: p.C = p.c
    trait A
    trait B
    class ann extends scala.annotation.StaticAnnotation
    class H[M[_]]
  }

  trait TypesBase {
    class X
    val x: X = ???
  }

  class Types[T] extends TypesBase {
    import TypesHelpers._
    override val x: X = new X

    val typeRef1: C = ??? : C
    val typeRef2: p.C = ??? : p.C
    val typeRef3: E#C = ??? : E#C
    val typeRef4: List[Int] = ??? : List[Int]
    val typeRef5: X = ??? : X
    val typeRef6: T = ??? : T
    val typeRef7: () => T = ??? : () => T
    val typeRef8: T => T = ??? : T => T
    val typeRef9: (T, T) => T = ??? : (T, T) => T
    val typeRef10: (T, T) = ??? : (T, T)

    val singleType1: c.type = ??? : c.type
    val singleType2: p.c.type = ??? : p.c.type
    // TODO: RscCompat regression
    // val Either = ??? : scala.util.Either.type

    // TODO: RscCompat regression
    // val thisType1 = ??? : this.type
    // val thisType2 = ??? : Types.this.type

    // FIXME: https://github.com/twitter/rsc/issues/143
    // val superType1 = ??? : super.x.type
    // val superType2 = ??? : super[TypesBase].x.type
    // val superType3 = ??? : Types.super[TypesBase].x.type

    // FIXME: https://github.com/twitter/rsc/issues/145
    // val constantType1 = ??? : 42.type

    val compoundType1: AnyRef = ??? : { def k: Int }
    val compoundType2: A with B = ??? : A with B
    val compoundType3: A with B = ??? : A with B { def k: Int }
    val compoundType4: compoundType4 = new compoundType4
    class compoundType4 extends { def k: Int = ??? }
    val compoundType5: A with B = new A with B
    val compoundType6: compoundType6 = new compoundType6
    class compoundType6 extends A with B { def k: Int = ??? }
    val compoundType7: A with List[_] with B = ??? : A with (List[T] forSome { type T }) with B

    // FIXME: https://github.com/twitter/rsc/issues/317
    // val annType1 = ??? : C @ann

    val existentialType1: Any = ??? : T forSome { type T }
    val existentialType2: List[Any] = ??? : List[_]

    // TODO: RscCompat regression
    // val universalType1 = ??? : H[({ type L[U] = List[U] })#L]

    val byNameType: (=> Any) => Any = ??? : ((=> Any) => Any)
    // FIXME: https://github.com/twitter/rsc/issues/144
    // val repeatedType = ??? : ((Any*) => Any)
  }

  implicit val implicit_x: Int = 42
  implicit val implicit_y: String = "42"
  trait In
  trait Out1
  trait Out2
  implicit val implicit_bijection1: ImplicitBijection[In, Out1] = ???
  implicit val implicit_bijection2: ImplicitBijection[In, Out2] = ???

  class Bugs {
    // TODO: RscCompat regression
    // val Either = scala.util.Either

    implicit def order[T]: Ordering[List[T]] = new Ordering[List[T]] {
      def compare(a: List[T], b: List[T]): Int = ???
    }

    val gauges: List[Int] = {
      val local1 = 42
      val local2 = 43
      List(local1, local2)
    }

    val clazz: Class[_] = Class.forName("foo.Bar")

    val compound: AnyRef = ??? : { def m(x: Int): Int }

    val loaded: List[Class[_]] = List[Class[_]]()

    val ti: Types[Int] = ???
    val innerClass1: Types[String]#X = new Types[String]().x
    val innerClass2: Types[Int]#X = ??? : Types[Int]#X
    val innerClass3: ti.X = ti.x
    val innerClass4: Base64.Decoder = Base64.getMimeDecoder

    val param: param = new param
    class param extends { lazy val default = true }
    val more1: more1 = new more1
    class more1 extends { var x = 42 }
    val more2: more2 = new more2
    class more2 extends { def foo(implicit x: Int, y: Int) = 42 }
    val more3: more3 = new more3
    class more3 extends { implicit def bar = 42 }

    implicit val crazy1 = implicitly[Int]
    implicit val crazy2: Bijection[In,Out1] = Bijection.connect[In, Out1]
    val sane1: String = implicitly[String]
    val sane2: Bijection[In,Out2] = Bijection.connect[In, Out2]

    // FIXME: https://github.com/scalameta/scalameta/issues/1872#issuecomment-498705622
    // val X, List(y) = List(1, 2)

    val t: T = ??? : foo.T

    val (a: String, b: String, c) = ("foo", "bar", "baz")
  }
}

private class RscCompat_Test {
  def x1 = ""
}

package object foo {
  class T
}
