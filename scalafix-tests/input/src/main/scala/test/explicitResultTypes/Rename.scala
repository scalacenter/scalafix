/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

import scala.collection.immutable.{List => LList}
import java.lang.{Boolean => JBoolean}

object Rename {
  type List = Int
  def foo(a: Int*) = identity(a.toList)
  def foo = identity(JBoolean.TRUE)
}
