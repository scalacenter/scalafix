/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1128
object Tuple1ExplicitResultTypes {
  def foo = {
    Tuple1(3)
  }
  def bar = {
    Tuple1(Tuple1(3))
  }
}
