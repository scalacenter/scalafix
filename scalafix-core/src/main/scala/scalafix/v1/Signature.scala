package scalafix.v1
// scalafmt: { maxColumn = 200 }

import scalafix.internal.util.Pretty

sealed abstract class Signature extends Product with Serializable {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoSignature
  final def nonEmpty: Boolean = !isEmpty
}

case object NoSignature extends Signature
final case class ClassSignature(typeParameters: List[SymbolInfo], parents: List[ScalaType], self: ScalaType, declarations: List[SymbolInfo]) extends Signature
final case class MethodSignature(typeParameters: List[SymbolInfo], parameterLists: List[List[SymbolInfo]], returnType: ScalaType) extends Signature
final case class TypeSignature(typeParameters: List[SymbolInfo], lowerBound: ScalaType, upperBound: ScalaType) extends Signature
final case class ValueSignature(tpe: ScalaType) extends Signature
