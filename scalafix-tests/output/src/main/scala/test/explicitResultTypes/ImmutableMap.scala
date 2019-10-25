package test.explicitResultTypes

import scala.collection.concurrent.Map
import scala.collection.immutable

object ImmutableMap {
  def foo: immutable.Map[Int,Int] = null.asInstanceOf[scala.collection.immutable.Map[Int, Int]]
  def bar: Map[Int,Int] = null.asInstanceOf[Map[Int, Int]]
}