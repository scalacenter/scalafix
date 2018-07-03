/*
rules = Disable
Disable.symbols = [
  {
    regex = "scala.collection.mutable.*"
    message = "use immutable structures"
  }
  {
    regex = ".*.toString"
  }
]

Disable.unlessInside = [
  {
    safeBlocks = "test.DisableRegex.IO"
    symbols = [
      {
        regex = {
          includes = "java.io.*"
          excludes = "java.io.InputStream"
        }
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

  @SuppressWarnings(Array("Disable.ListBuffer"))
  val buffer = new ListBuffer[Int]()

  @SuppressWarnings(Array("Disable.HashMap"))
  val abc = new mutable.HashMap[String, String]()

  @SuppressWarnings(Array("Disable.ListBuffer", "Disable.iterator", "Disable.toString"))
  val buffer2 = new mutable.ListBuffer[Int]().iterator.toString

  object IO { // IO we deserve
    def apply[T](run: => T): Nothing = ???
  }

  System.out.println("I love to debug by console messages.") // assert: Disable.println
  IO {
    System.out.println("[IMPORTANT MESSAGE]") // ok
  }
  System.in.read() // ok
}
