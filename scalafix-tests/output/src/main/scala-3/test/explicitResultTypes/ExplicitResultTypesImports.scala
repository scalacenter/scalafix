
package test.explicitResultTypes

import scala.util._
import scala.collection.immutable.ListSet
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.FiniteDuration
import java.util.TimeZone
import scala.collection.Searching.SearchResult
import java.util.Locale.Category

object ExplicitResultTypesImports {

  val x: ListSet[Int] = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone: TimeZone = null.asInstanceOf[java.util.TimeZone]

  val inner: SearchResult = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum: Category = java.util.Locale.Category.DISPLAY

  // TODO: respect type aliases?
  type MyResult = Either[Int, String]
  val inferTypeAlias: Either[Int, String] = null.asInstanceOf[Either[Int, String]]

  val wildcardImport: Try[Int] = Try(1)

  sealed abstract class ADT
  object ADT {
    case object A extends ADT
    case object B extends ADT
  }
  val productWithSerializable: List[ADT] = List(ADT.A, ADT.B)

  sealed abstract class ADT2
  trait Mixin[T]
  object ADT2 {
    case object A extends ADT2 with Mixin[Int]
    case object B extends ADT2 with Mixin[String]
    case object C extends ADT2 with Mixin[Int]
  }
  val longSharedParent1: List[ADT2 & Mixin[? >: Int & String <: Int | String]] = List(ADT2.A, ADT2.B)
  val longSharedParent2: List[ADT2 & Mixin[Int]] = List(ADT2.A, ADT2.C)

  val juMap: java.util.Map[Int, String] = java.util.Collections.emptyMap[Int, String]()
}