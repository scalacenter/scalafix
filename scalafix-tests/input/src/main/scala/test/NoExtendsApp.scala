/*
rewrites = NoExtendsApp
*/
package test

object Main extends App {
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

trait Something
object Main2 extends Something with App{
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

object Main3 extends App with Something{
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

object Main4 extends App with Something

object Main5 extends App with Something {}

object Main6
  extends App
  with Something

object Main7
  extends Something
  with App

object Main8
  extends Something with
  App

object Main9 extends
  App with
  Something

import scala.{ App => Ppa }
object Main10 extends Ppa with Something {
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}
