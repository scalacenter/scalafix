/*
rules = RemoveUnused
*/
package test.removeUnused

object RemoveUnusedTermsSignificantIndentation:

  private val a = // lost
    println(5)

  private var b1 =
    println("foo")
    1

  private val b2 =
    { println("foo") }
    { 1 }

  private
  var
  b3:
  Integer
  = // preserved
    // preserved
    println("foo")
    1