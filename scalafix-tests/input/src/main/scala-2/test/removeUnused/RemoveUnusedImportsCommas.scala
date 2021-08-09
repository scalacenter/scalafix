/*
rules = RemoveUnused
 */
package test.removeUnused

import Unused.b, Unused.c, Unused.d
import scala.util.Success  , scala.util.Failure
import scala.concurrent.Future,
       scala.concurrent.Await,
       scala.concurrent.TimeoutException

import scala.math.{
  min
  , E
  , Pi
}

object RemoveUnusedImportsCommas {
  println(b + d + Pi)
  Failure(???)
  null.asInstanceOf[TimeoutException]
}

object Unused {
  val b = 1
  val c = 1
  val d = 1
}
