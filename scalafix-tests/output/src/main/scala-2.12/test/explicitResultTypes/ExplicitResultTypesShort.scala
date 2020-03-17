
package test.explicitResultTypes

import scala.language.implicitConversions

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.Success
import scala.collection.immutable.ListSet

object ExplicitResultTypesShort {
  implicit val x: List[Map[Int,Set[String]]] = Nil
  implicit val y: HashMap[String,Success[ListBuffer[Int]]] = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int): List[String] = Nil
  implicit var zz: ListSet[String] = scala.collection.immutable.ListSet.empty[String]
  implicit val FALSE: Any => Boolean = (x: Any) => false
  implicit def tparam[T](e: T): T = e
  implicit val opt: Option[Int] = None
  implicit val seq: Seq[List[Int]] = Nil
  object Shadow {
    val Option = scala.collection.mutable.ListBuffer
    implicit val shadow: ListBuffer[List[Int]] = Option.empty[List[Int]]
  }
}
