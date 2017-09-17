/*
rules = ExplicitResultTypes
ExplicitResultTypes.unsafeShortenNames = true
 */
package test.explicitResultTypes

import scala.language.implicitConversions

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.Success

object ExplicitResultTypesShort {
  implicit val x = List.empty[Map[Int, Set[String]]]
  implicit val y = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int) = List.empty[String]
  implicit var zz = scala.collection.immutable.ListSet.empty[String]
  implicit val FALSE = (x: Any) => false
  implicit def tparam[T](e: T) = e
}
