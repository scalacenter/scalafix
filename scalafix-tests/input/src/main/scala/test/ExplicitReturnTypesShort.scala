/*
rewrites = ExplicitReturnTypes
explicitReturnTypes.unsafeShortenNames = true
 */
package test

import scala.language.implicitConversions

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.Success

object ExplicitReturnTypesShort {
  implicit val x = List.empty[Map[Int, Set[String]]]
  implicit val y = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int) = List.empty[String]
  trait Y { type X; def x: X }
  implicit def pathDependent(x: Y) = {
    implicit val result: x.X = x.x
    result
  }
  implicit val p = pathDependent(new Y { type X = Int; def x = 2 })
  implicit val FALSE = (x: Any) => false
}
