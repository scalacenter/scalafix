/*
rules = "ExplicitResultTypes"
 */
package rsc.tests

object ExplicitResultTypesImports {

  val x = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1 = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2 = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone = null.asInstanceOf[java.util.TimeZone]

  // TODO: Is this desirable behavior?
  val inner = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum = java.util.Locale.Category.DISPLAY
}
