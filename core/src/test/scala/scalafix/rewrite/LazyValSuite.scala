package scalafix.rewrite

import scala.meta.inputs.Input

class LazyValSuite extends RewriteSuite(VolatileLazyVal) {

  check(
    "basic",
    """|object a {
       |
       |val foo = 1
       |
       |  private lazy val x = 2
       |  lazy val x = 2
       |  @volatile lazy val dontChangeMe = 2
       |
       |  class foo {
       |    lazy val z = {
       |      reallyHardStuff()
       |    }
       |  }
       |}
    """.stripMargin,
    """|object a {
       |
       |val foo = 1
       |
       |  @volatile private lazy val x = 2
       |  @volatile lazy val x = 2
       |  @volatile lazy val dontChangeMe = 2
       |
       |  class foo {
       |    @volatile lazy val z = {
       |      reallyHardStuff()
       |    }
       |  }
       |}
    """.stripMargin
  )
}
