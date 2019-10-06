/*
rules = "ExplicitResultTypes"
 */
package tests

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}
import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}

object ExplicitResultTypesBug {
  type Seq = Int
  def foo(a: Int*) = a
  def foo = JBoolean.TRUE

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
}

