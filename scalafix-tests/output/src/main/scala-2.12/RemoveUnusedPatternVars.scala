package test

case class AB(a: Int, b: String)

case class XY(x: Int, y: Int)
case class YZ(xy: XY, z: String)

object Unusedmatchargs {
  // Add code that needs fixing here.
  val example = AB(42, "lol")

  example match {
    case AB(_, _) => println("Not used, good")
  }
  example match {
    case AB(_, _) => println("Not used, wrong")
  }
  example match {
    case AB(_, _) => println("b is not used, wrong")
  }
  example match {
    case AB(_, _) => println("a is not used, wrong")
  }

  example match {
    case AB(a, b) => println(s"$a $b used, good")
  }
  example match {
    case AB(_, b) => println(s"$b is used, good")
  }
  example match {
    case AB(a, _) => println(s"$a is used, good")
  }

  example match {
    case AB(a, _) if a < Int.MaxValue =>
      println("Do not delete")
  }

  example match {
    case AB(_, _) =>
      val a = 5
      println(a)
  }

  val anotherExample = YZ(XY(1, 2), "3")

  anotherExample match {
    case YZ(el, _) =>
      el match {
        case XY(b, _) => {
          println(b)
        }
      }
  }
}
