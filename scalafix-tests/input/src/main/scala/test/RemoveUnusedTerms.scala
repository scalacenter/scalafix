/*
rule = RemoveUnusedTerms
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
}
