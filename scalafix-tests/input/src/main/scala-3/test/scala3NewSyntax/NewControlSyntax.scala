/*
rules = [
  "Scala3NewSyntax"
]
*/
package scala3NewSyntax

object NewControlSyntax:
  def sign(x: Int) =
    if (x < 0)
      "negative"
    else if (x == 0)
      "zero"
    else
      "positive"

  def bool(x: Int) = if (x < 0) -x else x

  def square(xs: List[Int]): List[Int] =
    for (x <- xs if x > 0)
    yield x * x

  def addition(xs: List[Int], ys: List[Int]): Unit =
    for {
      x <- xs
      y <- ys
    } println(x + y)
