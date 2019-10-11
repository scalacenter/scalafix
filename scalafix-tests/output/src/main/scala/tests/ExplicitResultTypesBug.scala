package tests

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}
import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}
import java.{util => ju}

object ExplicitResultTypesBug {
  type Seq = Int
  def foo(a: Int*): SSeq[Int] = a
  def foo: JBoolean = JBoolean.TRUE

  class MyMirror(owner: ClassMirror) {
    val symbol: reflect.runtime.universe.MethodSymbol =
      owner.symbol.info.decl(TermName("")).asMethod
  }

  val map: mut.Map[Int,Int] = mut.Map.empty[Int, Int]

  object Ignored {
    import java.{util => ju}
    val hasImport: ju.List[Int] = ju.Collections.emptyList[Int]()
  }
  val missingImport: ju.List[Int] = java.util.Collections.emptyList[Int]()

  def o3: rsc.tests.testpkg.O3 = new rsc.tests.testpkg.O3()

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

  class Eval() {
    def inPlace[T](e: String): T = apply[T](e)
    def apply[T](e: String): T = ???
  }
}

