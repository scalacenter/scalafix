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
}

