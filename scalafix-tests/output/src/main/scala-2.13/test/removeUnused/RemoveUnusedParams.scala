package test.removeUnused

object UnusedParams {
  val f: String => Unit = _ => println("f")
  val ff = (_: String) => println("f")
  def g(x: String => Unit): Unit = ???
  g{_ => println("g")}
}
