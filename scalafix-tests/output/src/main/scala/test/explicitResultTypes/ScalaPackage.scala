package test.explicitResultTypes

object ScalaPackage {
  class Try
  def a: scala.util.Try[Int] = null.asInstanceOf[scala.util.Try[Int]]
}