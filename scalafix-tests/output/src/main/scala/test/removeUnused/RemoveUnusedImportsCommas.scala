package test.removeUnused

import Unused.b, Unused.d
import scala.util.Failure
import scala.concurrent.TimeoutException

import scala.math.Pi

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
