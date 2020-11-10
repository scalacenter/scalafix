/*
rules = "ExplicitResultTypes"
ExplicitResultTypes.skipSimpleDefinitions = ["Lit"]
 */
package rsc.tests

import scala.util._

object ExplicitResultTypesImports {

  val x = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1 = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2 = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone = null.asInstanceOf[java.util.TimeZone]

  val inner = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum = java.util.Locale.Category.DISPLAY

  // TODO: respect type aliases?
  type MyResult = Either[Int, String]
  val inferTypeAlias = null.asInstanceOf[Either[Int, String]]

  val wildcardImport = Try(1)

  sealed abstract class ADT
  object ADT {
    case object A extends ADT
    case object B extends ADT
  }
  val productWithSerializable = List(ADT.A, ADT.B)

  sealed abstract class ADT2
  trait Mixin[T]
  object ADT2 {
    case object A extends ADT2 with Mixin[Int]
    case object B extends ADT2 with Mixin[String]
    case object C extends ADT2 with Mixin[Int]
  }
  val longSharedParent1 = List(ADT2.A, ADT2.B)
  val longSharedParent2 = List(ADT2.A, ADT2.C)

  val juMap = java.util.Collections.emptyMap[Int, String]()
}
