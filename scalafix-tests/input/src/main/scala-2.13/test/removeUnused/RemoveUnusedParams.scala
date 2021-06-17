/*
rule = RemoveUnused
 */
package test.removeUnused

object UnusedParams {
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
}
