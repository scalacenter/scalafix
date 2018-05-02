package test.explicitResultTypes

import test.explicitResultTypes.\/
trait \/[A, B]

object ExplicitResultTypesInfix {
  val ab: Int \/ String = null.asInstanceOf[Int \/ String]
  def foo: Int \/ String = ab
}
