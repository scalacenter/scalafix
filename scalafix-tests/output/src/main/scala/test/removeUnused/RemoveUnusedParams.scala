package test.removeUnused

object UnusedParams {
  val f: String => Unit = _ => println("f")
  val ff = (_: String) => println("f")
  val fs = (used: String, _: Long) => println(used)
  def g(x: String => Unit): Unit = ???
  g{_ => println("g")}
}
