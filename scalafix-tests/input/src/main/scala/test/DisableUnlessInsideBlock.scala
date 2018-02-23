/*
rules = [
  Disable
]

Disable.unlessInsideBlock = [
  {
    safeBlock = "test.DisableUnlessInsideBlock.IO"
    symbols = [
      {
        symbol = "scala.Predef.println"
        message = "println has side-effects"
      }
      "java.lang.System.currentTimeMillis"
    ]
  }
  {
    safeBlock = "scala.Option"
    symbols = [
      {
        symbol = "test.DisableUnlessInsideBlock.dangerousFunction"
        message = "the function may return null"
      }
    ]
  }
]
*/
package test

object DisableUnlessInsideBlock {
  object IO { // IO we deserve
    def apply[T](run: => T): Nothing = ???
  }

  println("hi") // assert: Disable.println
  System.currentTimeMillis() // assert: Disable.currentTimeMillis
  IO.apply {
    println("hi") // ok
  }
  IO(println("hi")) // ok
  IO {
    println("hi") // ok
  }
  IO {
    {
      println("hi") // ok
      System.currentTimeMillis() // ok
    }
  }
  IO {
    val a = 1
    val b = {
      println("hi") // ok
      2
    }
  }
  IO {
    def sideEffect(i: Int) = println("not good!") // assert: Disable.println
    (i: Int) => println("also not good!") // assert: Disable.println
  }
  IO {
    class SideEffect {
      def oooops = println("I may escape!") // assert: Disable.println
    }

    new SideEffect() // ok
  }


  class Foo
  def dangerousFunction(): Foo = null // assert: Disable.dangerousFunction

  dangerousFunction() // assert: Disable.dangerousFunction
  Option {
    println("not here") // assert: Disable.println
    dangerousFunction() // ok
  }
  Option.apply(dangerousFunction()) // ok
  Option(dangerousFunction()) // ok

  IO {
    Option(dangerousFunction()) // ok
  }

  Option(IO(println("boo!"))) // ok
}
