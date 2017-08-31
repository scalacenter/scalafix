package test

import scala.language.implicitConversions

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.Success

object ExplicitReturnTypesShort {
  implicit val x: List[Map[Int, Set[String]]] = List.empty[Map[Int, Set[String]]]
  implicit val y: HashMap[String, Success[ListBuffer[Int]]] = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int): scala.collection.immutable.List[scala.Predef.String] = List.empty[String]
  trait Y { type X; def x: X }
  implicit def pathDependent(x: Y): x.X = {
    implicit val result: x.X = x.x
    result
  }
  implicit val p: Int = pathDependent(new Y { type X = Int; def x = 2 })
}
