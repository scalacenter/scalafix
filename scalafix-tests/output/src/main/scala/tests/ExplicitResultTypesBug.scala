package tests

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}
import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}

object ExplicitResultTypesBug {
  type Seq = Int
  def foo(a: Int*): SSeq[Int] = a
  def foo: JBoolean = JBoolean.TRUE

  class MyMirror(owner: ClassMirror) {
    val symbol: reflect.runtime.universe.MethodSymbol =
      owner.symbol.info.decl(TermName("")).asMethod
  }

  val map: mut.Map[Int,Int] = mut.Map.empty[Int, Int]
}

