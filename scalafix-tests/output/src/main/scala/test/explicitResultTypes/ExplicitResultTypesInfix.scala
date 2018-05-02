package test.explicitResultTypes

import test.explicitResultTypes.{ ExplicitResultTypesInfix, \/ }
trait \/[A, B]

object ExplicitResultTypesInfix {
  val ab: Int \/ String = null.asInstanceOf[Int \/ String]
  def foo: Int \/ String = ab

  case class :+:[T1, T2]()
  def bar: ExplicitResultTypesInfix.:+:[Double, Long] = :+:[Double, Long]()
}
