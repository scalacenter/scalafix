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
  @SuppressWarnings(Array("Disable.<init>"))
  val buffer = new ListBuffer[Int]()

  @SuppressWarnings(Array("Disable.mutable"))
  @SuppressWarnings(Array("Disable.HashMap"))
  @SuppressWarnings(Array("Disable.<init>"))
  val abc = new mutable.HashMap[String, String]()

  object IO { // IO we deserve
    def apply[T](run: => T): Nothing = ???
  }

  System.out.println("I love to debug by console messages.") // assert: Disable.println
  IO {
    System.out.println("[IMPORTANT MESSAGE]") // ok
  }
  System.in.read() // ok
}
