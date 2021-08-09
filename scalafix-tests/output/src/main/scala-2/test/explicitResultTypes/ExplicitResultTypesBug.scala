package test.explicitResultTypes

import scala.collection.{mutable => mut}
import scala.reflect.runtime.universe._
import java.{util => ju}
import test.explicitResultTypes.testpkg.{ O3, O4 }

object ExplicitResultTypesBug {

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

  def o3: O3 = new test.explicitResultTypes.testpkg.O3()

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
  abstract class Opt[T] {
    def get(e: T): T
  }
  class IntOpt extends Opt[Int] {
    def get(e: Int): Int = e
  }

  val o4: List[O4] = null.asInstanceOf[List[test.explicitResultTypes.testpkg.O4]]

}
