package test

import scala.util.control.NonFatal
import scala.concurrent.Future
import scala.util.{Properties, Try}
import scala.util.{Success => Successful}

object RemoveUnusedImports {
  val NonFatal(a) = new Exception
  Future.successful(1)
  Properties.ScalaCompilerVersion
  Try(1)
  Successful(1)
}