/*
rules = RemoveUnused
*/
package test.removeUnused

class A {
  private val a = 1
  private def a2 = 1
  private var a3 = 1
}

object B {
  private val b = 1
  private def b2 = 1
  private var b3 = 1
}

package object C {
  private val c = 1
  private def c2 = 1
  private var c3 = 1
}

trait D {
  private val d = 1
  private def d2 = 1
  private var d3 = 1
}
