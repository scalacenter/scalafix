/*
rule = RemoveUnusedImports
 */
package test

import scala.sys.process.{
  FileProcessLogger,
  ProcessBuilder
}
import scala.math.{
  Ordered,
  Pi,
  E
}

object RemoveUnusedImports2 {
  val x: FileProcessLogger = ???
  println(Ordered.orderingToOrdered(Pi))
}
