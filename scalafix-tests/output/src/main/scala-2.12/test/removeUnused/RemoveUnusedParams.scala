package test

object UnusedParams {
  def g(x: String => Unit): Unit = ???
  g{_ => println("g")}
}
