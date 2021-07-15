
package scala3NewSyntax

object NewControlSyntax:
  def sign(x: Int) =
    if x < 0 then
      "negative"
    else if x == 0 then
      "zero"
    else
      "positive"

  def bool(x: Int) = if x < 0 then -x else x

  def square(xs: List[Int]): List[Int] =
    for x <- xs if x > 0
    yield x * x

  def addition(xs: List[Int], ys: List[Int]): Unit =
    for
      x <- xs
      y <- ys
    do
      println(x + y)
