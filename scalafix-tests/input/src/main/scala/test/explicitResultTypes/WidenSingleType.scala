/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

trait WidenSingleType {
  object param extends Ordering[Int] {
    def compare(x: Int, y: Int): Int = ???
  }
}
object WidenSingleType {
  def list(a: WidenSingleType, b: WidenSingleType) =
    Seq(a.param, b.param)
}