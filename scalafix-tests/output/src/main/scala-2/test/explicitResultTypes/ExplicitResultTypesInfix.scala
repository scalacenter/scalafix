
package test.explicitResultTypes

trait \/[A, B]

object ExplicitResultTypesInfix {
  val ab: Int \/ String = null.asInstanceOf[Int \/ String]
  def foo: Int \/ String = identity(ab)

  case class :+:[T1, T2]()
  def bar: Double :+: Long = :+:[Double, Long]()
}
