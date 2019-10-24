/*
rules = "ExplicitResultTypes"
 */
package tests

import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}

object ExplicitResultTypesBug {

  class MyMirror(owner: ClassMirror) {
    val symbol =
      owner.symbol.info.decl(TermName("")).asMethod
  }

  val map = mut.Map.empty[Int, Int]

  object Ignored {
    import java.{util => ju}
    val hasImport = ju.Collections.emptyList[Int]()
  }
  val missingImport = java.util.Collections.emptyList[Int]()

  def o3 = new rsc.tests.testpkg.O3()

  def overload(a: Int) = a
  def overload(a: String) = a

  abstract class ParserInput {
    def apply(index: Int): Char
  }
  case class IndexedParserInput(data: String) extends ParserInput {
    override def apply(index: Int) = data.charAt(index)
  }
  case class Foo(a: Int) {
    def apply(x: Int) = x
  }

  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }
  object Overrides extends Trait {
    val foo = Map.empty
    val message: CharSequence = s"hello $foo"
  }

  abstract class Opt[T] {
    def get(e: T): T
  }
  class IntOpt extends Opt[Int] {
    def get(e: Int) = e
  }

  val o4 = null.asInstanceOf[List[rsc.tests.testpkg.O4]]

}
