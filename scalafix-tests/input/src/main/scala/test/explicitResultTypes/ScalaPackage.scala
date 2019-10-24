/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

object ScalaPackage {
  class Try
  def a = null.asInstanceOf[scala.util.Try[Int]]
}