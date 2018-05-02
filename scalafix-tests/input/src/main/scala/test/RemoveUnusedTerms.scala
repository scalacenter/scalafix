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
    val xy = 42 // scalafix:ok RemoveUnusedTerms
  }

  val cc = 0

  Some(1) match {
      case Some(x) => 2
  }

  List(1) match {
      case List(x @ _*) => 1
  }
}
