/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

trait WidenSingleType {
  object param extends Ordering[Int] {
    def compare(x: Int, y: Int): Int = ???
  }
  object strings {
    val message = "Hello!"
  }
}
abstract class WidenSingleTypeUsage {
  def widen: WidenSingleType
  def widenString = widen.strings // left un-annotated
}
object WidenSingleType {
  def list(a: WidenSingleType, b: WidenSingleType) =
    Seq(a.param, b.param)
  def strings(a: WidenSingleType) = a.strings
}