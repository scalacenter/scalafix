package scalafix.v1

import scalafix.internal.util.Pretty

sealed abstract class Signature extends Product with Serializable {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoSignature
  final def nonEmpty: Boolean = !isEmpty
}

final case class ValueSignature(tpe: SemanticType) extends Signature
final case class ClassSignature(
    typeParameters: List[SymbolInformation],
    parents: List[SemanticType],
    self: SemanticType,
    declarations: List[SymbolInformation])
    extends Signature
final case class MethodSignature(
    typeParameters: List[SymbolInformation],
    parameterLists: List[List[SymbolInformation]],
    returnType: SemanticType)
    extends Signature
final case class TypeSignature(
    typeParameters: List[SymbolInformation],
    lowerBound: SemanticType,
    upperBound: SemanticType)
    extends Signature
case object NoSignature extends Signature
