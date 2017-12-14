/*
rules = [
  DisableUnless
]

DisableUnless.symbols = [
  {
    mode = "outside"
    block = "test.fpSpec.NestedBlockingSpec.IO"
    symbol = "scala.Predef.println"
    message = "println has side-effects"
  }
]
*/
package test.fpSpec

object NestedBlockingSpec {
  object IO { // IO we deserve
    def apply[T](run: => T): Nothing = ???
  }

  println("hi") // assert: DisableUnless.println
  IO {
    println("hi") // ok
  }
  IO.apply {
    println("hi") // ok
  }
  IO.apply {
    {
      println("hi") // ok
    }
  }
  IO.apply {
    val a = 1
    val b = {
      println("hi") // ok
      2
    }
  }
}
