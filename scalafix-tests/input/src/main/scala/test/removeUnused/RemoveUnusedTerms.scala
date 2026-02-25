/*
rules = RemoveUnused
 */
package test.removeUnused

object RemoveUnusedTerms {

  def foo = {
    val a = "unused"
    val aa = // lost
      println(5)
    var b = 0
    var bb = println(0)
    println(1)
    def c = "unused"
    def cc = println(5.0)
    val xy = 42 // scalafix:ok RemoveUnusedTerms
    // https://github.com/scalacenter/scalafix/issues/2061
    val d = (3 + 4)
    val e: Int =
      (3
        + 4)
    val f = ((3 + 4))
    var g = (println(0))
    val h = (if (true) 1 else 2)
  }

  val dd = 0
  def f(x: Int) = "unused"
  private def ff(x: Int) = "unused"

  private val b1: Integer = { println("foo"); 1 }
  private var b2: Integer = /* preserved */ {
    println("foo")
    1
  }
  private
  var
  b3:
  Integer
  = /* preserved */
    // preserved
    {
      println("foo")
      1
    }
}
