package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1128
object Tuple1ExplicitResultTypes {
  def foo: Tuple1[Int] = {
    Tuple1(3)
  }
  def bar: Tuple1[Tuple1[Int]] = {
    Tuple1(Tuple1(3))
  }
  def baz: List[Tuple1[Int]] = List(Tuple1(3))
}
