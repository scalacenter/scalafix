package test.removeUnused

object RemoveUnusedTermsSignificantIndentation:

  println(5)

  locally:
    println("foo")
    1

  locally:
    { println("foo") }
    { 1 }

  locally: // preserved
    // preserved
    println("foo")
    1