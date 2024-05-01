// scalafix:off
package test

object DenotationOpsTest {
  def m(x: Int, y: String): List[String] = List(y)
  var x = true
  private val y = m(42, "hey")
}
