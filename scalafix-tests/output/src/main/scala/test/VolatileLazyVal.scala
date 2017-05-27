package test

class VolatileLazyVal {
  @volatile lazy val x = 2
  @volatile lazy val dontChangeMe = 2
  @volatile private lazy val y = 2

  class foo {
    @volatile lazy val z = {
      println()
    }
  }

}
