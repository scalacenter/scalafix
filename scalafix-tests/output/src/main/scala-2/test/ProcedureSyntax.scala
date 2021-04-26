package test

object ProcedureSyntax {
  // This is a comment
  def main(args: Seq[String]): Unit = {
    var number = 2
    def increment(n: Int): Unit = {
      number += n
    }
    increment(3)
    args.foreach(println)
  }
  def foo: Unit = {
    println(1)
  }
  def main(): Unit = /* unit */ {}
  def bar(args: (Int, Int)): Unit = {
    println(1)
  }
  def baz[A]: Unit = { println("baz[A]") }
  trait Decl { def unit: Unit }
}
