/*
rule = RemoveUnused
 */
package test

object RemoveUnusedTerms {

  def foo {
    val a = "unused"
    val aa = println(5)
    var b = 0
    var bb = println(0)
    println(1)
    def c = "unused"
    def cc = println(5.0)
    val xy = 42 // scalafix:ok RemoveUnusedTerms
  }

  val dd = 0
  def f(x: Int) = "unused"
  private def ff(x: Int) = "unused"

  def g(x: String => Unit): Unit = ???
  g{implicit string => println("g")}
  def pf(x: PartialFunction[Any, Unit]): Unit = ???
  case class A(a: Int)
  pf{
    case string: String => ???
    case (i: Int) => ???
    case (a: Int, b) => println(b)
    case a@A(v) => ???
    case x :: (y1, y2) :: Nil => println(x)
    case (zz) => ???
  }
  try ??? catch {case e: Exception => ???}
}
