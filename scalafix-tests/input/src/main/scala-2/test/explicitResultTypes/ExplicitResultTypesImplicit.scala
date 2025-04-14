/*
rules = ExplicitResultTypes
ExplicitResultTypes.memberKind = [Val, Def, Var]
ExplicitResultTypes.memberVisibility = [Public, Protected]
 */
package test.explicitResultTypes

import scala.language.implicitConversions

object ExplicitResultTypesImplicit {
  private implicit var j = 1
  implicit val L = List(1)
  implicit val M = Map(1 -> "STRING")
  implicit def D = 2
  implicit def tparam[T](e: T) = e
  implicit def tparam2[T](e: T) = List(e)
  implicit def tparam3[T](e: T) = Map(e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
}
