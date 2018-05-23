/*
rule = RemoveUnusedImports
 */
package test

import scala.util.control.NonFatal
import scala.concurrent.Future
import scala.util.{Properties, Try}
import scala.util.{Success => Successful}
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
  Successful(1)
  ExecutionContext.defaultReporter
  new RichBoolean(true)
  new TimeoutException
}
