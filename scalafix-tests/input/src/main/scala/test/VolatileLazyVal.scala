/*
rewrites = VolatileLazyVal
 */
package test

class VolatileLazyVal {
  lazy val x = 2
  @volatile lazy val dontChangeMe = 2
  private lazy val y = 2

  class foo {
    lazy val z = {
      println()
    }
  }

}
