package test

object Main {
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

trait Something
object Main2 extends Something{
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

object Main3 extends Something{
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

object Main4 extends Something

object Main5 extends Something {}

object Main6
  extends Something

object Main7
  extends Something

object Main8
  extends Something

object Main9 extends
  Something

import scala.{ App => Ppa }
object Main10 extends Something {
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}
