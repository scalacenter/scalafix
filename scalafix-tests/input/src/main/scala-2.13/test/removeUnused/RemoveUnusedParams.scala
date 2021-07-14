/*
rule = RemoveUnused
 */
package test.removeUnused

object UnusedParams {
  val f: String => Unit = unused => println("f")
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
}
