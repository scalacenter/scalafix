package tests

import scala.collection.{Seq => SSeq}
import java.lang.{Boolean => JBoolean}

object ExplicitResultTypesBug {
  type Seq = Int
  def foo(a: Int*): SSeq[Int] = a
  def foo: JBoolean = JBoolean.TRUE
}

