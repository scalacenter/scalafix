package scalafix.internal.v1

import scalafix.v1.NoType
import scalafix.v1.Symbol
import scalafix.v1.TypeRef

object Types {
  def scalaNothing: TypeRef =
    new TypeRef(NoType, Symbol("scala/Nothing#"), Nil)
  def scalaAny: TypeRef =
    new TypeRef(NoType, Symbol("scala/Any#"), Nil)
  def javaObject: TypeRef =
    new TypeRef(NoType, Symbol("java/lang/Object#"), Nil)
}
