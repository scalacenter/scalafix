package rsc.tests

import java.util.TimeZone
import java.util.Locale.Category
import scala.collection.Searching
import scala.collection.immutable.ListSet
import scala.concurrent.duration.FiniteDuration
object ExplicitResultTypesImports {

  val x: ListSet[Int] = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone: TimeZone = null.asInstanceOf[java.util.TimeZone]

  // TODO: Is this desirable behavior?
  val inner: Searching.SearchResult = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum: Category = java.util.Locale.Category.DISPLAY

  // TODO: respect type aliases?
  type MyResult = Either[Int, String]
  val inferTypeAlias: Either[Int,String] = null.asInstanceOf[Either[Int, String]]
}
