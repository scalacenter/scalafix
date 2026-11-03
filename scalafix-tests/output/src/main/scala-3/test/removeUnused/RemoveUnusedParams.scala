package test.removeUnused

object UnusedParams {
  val f: String => Unit = _ => println("f")
  val ff = (_: String) => println("f")
  val fs = (used: String, _: Long) => println(used)
  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
  g{implicit typed: String => println("g")}
  g{implicit /* a */ commented /* b */ : String => println("g")}
}
