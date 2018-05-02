/*
rules = ExplicitResultTypes
ExplicitResultTypes.memberKind = [Val, Def, Var]
ExplicitResultTypes.memberVisibility = [Public, Protected]
ExplicitResultTypes.unsafeShortenNames = true
*/
package test.explicitResultTypes

trait \/[A, B]

object ExplicitResultTypesInfix {
  val ab = null.asInstanceOf[Int \/ String]
  def foo = ab

  case class :+:[T1, T2]()
  def bar = :+:[Double, Long]()
}
