/*
rule = RemoveUnused
 */
package test

object UnusedParams {
  val f: String => Unit = unused => println("f")
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
}
