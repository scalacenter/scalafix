package rsc.tests

import java.util.TimeZone
import scala.collection.immutable.ListSet
import scala.concurrent.duration.FiniteDuration
object ExplicitResultTypesImports {

  val x: ListSet[Int] = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2: FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone: TimeZone = null.asInstanceOf[java.util.TimeZone]
}
