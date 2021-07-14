/*
rule = RemoveUnused
 */
package test

object UnusedParams {
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
}
