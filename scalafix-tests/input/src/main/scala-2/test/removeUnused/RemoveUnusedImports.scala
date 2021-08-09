/*
rules = RemoveUnused
 */
package test.removeUnused

import scala.util.control.NonFatal
import scala.concurrent.Future
import scala.util.{Properties, Try}
import scala.math.{ max, min }
import scala.util.{Success => Successful}
import scala.util.{ Random => _ }
import scala.util.{ Random => _, Left => _, Right => _}
import scala.concurrent.ExecutionContext
import scala.runtime.{RichBoolean}
import scala.concurrent.// formatting caveat
TimeoutException

//import scala.concurrent.{
//    CancellationException
//  , ExecutionException
//  , TimeoutException // ERROR
//}

object RemoveUnusedImports {
  val NonFatal(a) = new Exception
  Future.successful(1)
  println(Properties.ScalaCompilerVersion)
  Try(1)
  Successful(min(1, 2))
  ExecutionContext.defaultReporter
  new RichBoolean(true)
  new TimeoutException
}
