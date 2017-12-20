package test

object DottyAutoTuplingFunctionArgs {
  Some((1, 2)).map((a, b) => a + b)
  List((1, 2, 3), (4, 5, 6)).map { (a, b, c) => println(a); a + b + c }
  List((1, 2), (3, 4)).foreach { (a, b) => { val c = a + b; println(c) } }
  Set((1, 2, 3)).map { (a, b, c) =>
    println(a + b + c)
  }
  List((1, 1, 1, 1, 1), (2, 2, 2, 2, 2)).flatMap{
    (_, _, _, _, n) => List(n)
  }
  val xs = List("3", "1", "0").zipWithIndex
  xs.filter((w, i) => w == i.toString)

  // should not rewrite the below
  xs.map { case (w, i) if i > 1 => s"$w-$i" } // case with guards
  List((Some(1), Some(1))).map { case (Some(a), Some(b)) => a + b } // extraction
  xs.map {
    case (a, b) => a + b
    case _ => 0
  } // multiple cases
  xs.find { case (_, x @ b) => x - b == 0 } // binding
  xs.map { case ("0", 0) => "zero" } // literals
  List(("a", 1, false, 1.0), ("b", 2, true, 2.0)).exists{
    case (_: String, _: Int, c: Boolean, _: Double) => c
  } // typed parameters
  class A
  class B extends A
  def foo(xs: List[(A, A)]) = xs.map {
    case (x: B, y: A) => 1
  } // typed parameters not matching tuple element types
}
