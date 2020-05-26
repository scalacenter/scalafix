/*
rules = ExplicitSynthetic
 */
package test

object ExplicitSynthetic {
  val list = List(1)
  val apply = List.apply(1)
  def +[T](e: T): String = e.toString
  ExplicitSynthetic + 42
}