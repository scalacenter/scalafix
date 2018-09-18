package scalafix.v1
// scalafmt: { maxColumn = 120 }

import scalafix.internal.util.Pretty

/** Encoding of the Scala type system as a sealed data structure. */
sealed abstract class SemanticType extends Product with Serializable {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoType
  final def nonEmpty: Boolean = !isEmpty
}
final case class TypeRef(prefix: SemanticType, symbol: Symbol, typeArguments: List[SemanticType]) extends SemanticType
final case class SingleType(prefix: SemanticType, symbol: Symbol) extends SemanticType
final case class ThisType(symbol: Symbol) extends SemanticType
final case class SuperType(prefix: SemanticType, symbol: Symbol) extends SemanticType
final case class ConstantType(constant: Constant) extends SemanticType
final case class IntersectionType(types: List[SemanticType]) extends SemanticType
final case class UnionType(types: List[SemanticType]) extends SemanticType
final case class WithType(types: List[SemanticType]) extends SemanticType
final case class StructuralType(tpe: SemanticType, declarations: List[SymbolInformation]) extends SemanticType
final case class AnnotatedType(annotations: List[Annotation], tpe: SemanticType) extends SemanticType
final case class ExistentialType(tpe: SemanticType, declarations: List[SymbolInformation]) extends SemanticType
final case class UniversalType(typeParameters: List[SymbolInformation], tpe: SemanticType) extends SemanticType
final case class ByNameType(tpe: SemanticType) extends SemanticType
final case class RepeatedType(tpe: SemanticType) extends SemanticType
case object NoType extends SemanticType
