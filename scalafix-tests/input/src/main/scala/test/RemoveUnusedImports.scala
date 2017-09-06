/*
rule = RemoveUnusedImports
 */
package test

import scala.collection.mutable
import scala.util.control.{ NonFatal, Breaks }
import scala.concurrent.{Await, Future}
import scala.util.{Properties, DynamicVariable, Try}
import scala.util.{Success => Successful, Random}
import scala.collection.mutable.{Set ⇒ MutableSet, Map ⇒ MutableMap}
import scala.collection.mutable.{Seq => MutableSeq}
import scala.sys.process._
import scala.concurrent.{CancellationException, ExecutionException, ExecutionContext}
import scala.runtime.{RichBoolean}
import scala.concurrent.{ // formatting caveat
  CancellationException,
  ExecutionException,
  TimeoutException
}
//import scala.concurrent.{
//    CancellationException
//  , ExecutionException
//  , TimeoutException // ERROR
//}

object RemoveUnusedImports {
  import Future._
  val NonFatal(a) = new Exception
  Future.successful(1)
  println(Properties.ScalaCompilerVersion)
  Try(1)
  Successful(1)
  ExecutionContext.defaultReporter
  new RichBoolean(true)
  new TimeoutException
}
