package tests

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}
import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}
import java.{util => ju}
import rsc.tests.testpkg.{ O3, O4 }

object ExplicitResultTypesBug {
  type Seq = Int
  def foo(a: Int*): SSeq[Int] = a
  def foo: JBoolean = JBoolean.TRUE

  class MyMirror(owner: ClassMirror) {
    val symbol: MethodSymbol =
      owner.symbol.info.decl(TermName("")).asMethod
  }

  val map: mut.Map[Int,Int] = mut.Map.empty[Int, Int]

  object Ignored {
    import java.{util => ju}
    val hasImport: ju.List[Int] = ju.Collections.emptyList[Int]()
  }
  val missingImport: ju.List[Int] = java.util.Collections.emptyList[Int]()

  def o3: O3 = new rsc.tests.testpkg.O3()

  def overload(a: Int): Int = a
  def overload(a: String): String = a

  abstract class ParserInput {
    def apply(index: Int): Char
  }
  case class IndexedParserInput(data: String) extends ParserInput {
    override def apply(index: Int): Char = data.charAt(index)
  }
  case class Foo(a: Int) {
    def apply(x: Int): Int = x
  }

  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }
  object Overrides extends Trait {
    val foo: Map[Int,String] = Map.empty
    val message: CharSequence = s"hello $foo"
  }

  abstract class Opt[T] {
    def get(e: T): T
  }
  class IntOpt extends Opt[Int] {
    def get(e: Int): Int = e
  }

  val o4: List[O4] = null.asInstanceOf[List[rsc.tests.testpkg.O4]]

  trait ThisType  {
    def cp(): this.type
  }
  class ThisTypeImpl extends ThisType {
    def cp(): this.type = this
  }
}

