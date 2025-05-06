/*
rule = RemoveUnused
 */
package test.removeUnused

case class AB(aa: Int, bb: String)

case class XY(x: Int, y: Int)
case class YZ(xy: XY, z: String)

object ob {
  // Add code that needs fixing here.
  val example = AB(42, "lol")

  example match {
    case AB(aa, bb) => println("https://github.com/scala/bug/issues/13035")
  }
  example match {
    case AB(_, _) => println("Not used, good")
  }
  example match {
    case AB(a, b) => println("Not used, wrong")
  }
  example match {
    case AB(_, b) => println("b is not used, wrong")
  }
  example match {
    case AB(a, _) => println("a is not used, wrong")
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
    case AB(a, _) =>
      val a = 5
      println(a)
  }

  val anotherExample = YZ(XY(1, 2), "3")

  anotherExample match {
    case YZ(el, b) =>
      el match {
        case XY(b, _) => {
          println(b)
        }
      }
  }

  def pf(x: PartialFunction[Any, Unit]): Unit = ???
  case class A(a: Int)
  pf{
    case string: String => ???
    case (i: Int) => ???
    case (a: Int, b) => println(b)
    case a@A(v) => ???
    case x :: (y1, y2) :: Nil => println(x)
    case (zz) => ???
  }
  try ??? catch {case e: Exception => ???}

  def a: Unit = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }

  val b = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }
}

trait tr {
  // Add code that needs fixing here.
  val example = AB(42, "lol")

  example match {
    case AB(_, _) => println("Not used, good")
  }
  example match {
    case AB(a, b) => println("Not used, wrong")
  }
  example match {
    case AB(_, b) => println("b is not used, wrong")
  }
  example match {
    case AB(a, _) => println("a is not used, wrong")
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
    case AB(a, _) =>
      val a = 5
      println(a)
  }

  val anotherExample = YZ(XY(1, 2), "3")

  anotherExample match {
    case YZ(el, b) =>
      el match {
        case XY(b, _) => {
          println(b)
        }
      }
  }

  def a: Unit = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }

  val b = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }
}

class cl {
  // Add code that needs fixing here.
  val example = AB(42, "lol")

  example match {
    case AB(_, _) => println("Not used, good")
  }
  example match {
    case AB(a, b) => println("Not used, wrong")
  }
  example match {
    case AB(_, b) => println("b is not used, wrong")
  }
  example match {
    case AB(a, _) => println("a is not used, wrong")
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
    case AB(a, _) =>
      val a = 5
      println(a)
  }

  val anotherExample = YZ(XY(1, 2), "3")

  anotherExample match {
    case YZ(el, b) =>
      el match {
        case XY(b, _) => {
          println(b)
        }
      }
  }

  def a: Unit = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }

  val b = {
    example match {
      case AB(_, _) => println("Not used, good")
    }
    example match {
      case AB(a, b) => println("Not used, wrong")
    }
    example match {
      case AB(_, b) => println("b is not used, wrong")
    }
    example match {
      case AB(a, _) => println("a is not used, wrong")
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
      case AB(a, _) =>
        val a = 5
        println(a)
    }
  }

  def f(v: (Int, (Boolean, String))): Int = v match {
    case t @ (i, v @ (_, _)) => i
  }
}