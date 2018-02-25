/*
rules = Disable
Disable.symbols = [
  {
    symbol = "java.lang.Object.equals"
    banHierarchy = true
  }
]

Disable.unlessInsideBlock = [
  {
    safeBlock = "test.DisableRegex.IO"
    symbols = [
      {
        symbol = "scala.Option.get"
        banHierarchy = true
      }
    ]
  }
]
 */
package test

object DisableHierarchy {
  Array(1, 2, 3).equals(Array(5, 6, 7)) // assert: Disable.equals
  class A(i: Int) {
    override def equals(o: scala.Any): Boolean = super.equals(o) // assert: Disable.equals
  }
  new A(1).equals(new A(2)) // ok
}
