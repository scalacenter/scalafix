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
import scala.util, util.Sorting

object RemoveUnusedImports {
  val NonFatal(a) = new Exception
  Future.successful(1)
  println(Properties.ScalaCompilerVersion)
  Try(1)
  Successful(1)
  ExecutionContext.defaultReporter
  new RichBoolean(true)
  new TimeoutException
  Sorting.quickSort(List(2, 1, 3).toArray)
}
