package test.explicitResultTypes

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}

object Rename {
  type Seq = Int
  def foo(a: Int*): SSeq[Int] = a
  def foo: JBoolean = JBoolean.TRUE
}