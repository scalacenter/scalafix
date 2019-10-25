/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

import scala.collection.concurrent.Map

object ImmutableMap {
  def foo = null.asInstanceOf[scala.collection.immutable.Map[Int, Int]]
  def bar = null.asInstanceOf[Map[Int, Int]]
}