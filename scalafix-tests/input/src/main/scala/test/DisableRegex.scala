/*
rules = Disable
Disable.symbols = [
  {
    regex = "scala.collection.mutable.*"
    message = "use immutable structures"
  }
]

Disable.unlessInside = [
  {
    safeBlock = "test.DisableRegex.IO"
    symbols = [
      {
        regex = "java.io.*"
        message = "input/output are only allowed within IO block"
      }
    ]
  }
]
 */
package test

object DisableRegex {
  import scala.collection.mutable._ // ok
  import scala.collection.mutable   // ok
  val buffer = new ListBuffer[Int]() // assert: Disable.ListBuffer
  val abc = new mutable.HashMap[String, String]() // scalafix:ok Disable.mutable Disable.HashMap

  object IO { // IO we deserve
    def apply[T](run: => T): Nothing = ???
  }

  System.out.println("I love to debug by console messages.") // assert: Disable.println
  IO {
    System.out.println("[IMPORTANT MESSAGE]") // ok
  }
}
