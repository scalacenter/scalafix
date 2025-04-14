
package test.explicitResultTypes

import scala.language.implicitConversions

object ExplicitResultTypesImplicit {
  private implicit var j = 1
  implicit val L: List[Int] = List(1)
  implicit val M: Map[Int,String] = Map(1 -> "STRING")
  implicit def D = 2
  implicit def tparam[T](e: T) = e
  implicit def tparam2[T](e: T): List[T] = List(e)
  implicit def tparam3[T](e: T): Map[T,T] = Map(e -> e)
  class implicitlytrick {
    implicit val s: _root_.java.lang.String = "string"
    implicit val x = implicitly[String]
  }
}
