/*
rule = RemoveUnusedTerms
 */
package test

object RemoveUnusedTerms {

  def foo {
    val a = "unused"
    val aa = println(5)
    var b = println(0)
    println(1)
  }

  val unused = 0
}
