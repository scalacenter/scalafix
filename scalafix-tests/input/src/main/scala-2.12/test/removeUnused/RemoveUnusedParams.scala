/*
rule = RemoveUnused
 */
package test.removeUnused

object UnusedParams {
  val f: String => Unit = unused => println("f")
  val ff = (unused: String) => println("f")
  val fs = (used: String, unused: Long) => println(used)
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
}
