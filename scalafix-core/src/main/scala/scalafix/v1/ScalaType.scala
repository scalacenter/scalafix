package scalafix.v1
// scalafmt: { maxColumn = 120}

import scalafix.internal.util.Pretty

/** Encoding of the Scala type system as a sealed data structure. */
sealed abstract class ScalaType extends Product with Serializable {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoType
  final def nonEmpty: Boolean = !isEmpty
}
final case class TypeRef(prefix: ScalaType, symbol: Symbol, typeArguments: List[ScalaType]) extends ScalaType
final case class SingleType(prefix: ScalaType, symbol: Symbol) extends ScalaType
final case class ThisType(symbol: Symbol) extends ScalaType
final case class SuperType(prefix: ScalaType, symbol: Symbol) extends ScalaType
final case class ConstantType(constant: Constant) extends ScalaType
final case class IntersectionType(types: List[ScalaType]) extends ScalaType
final case class UnionType(types: List[ScalaType]) extends ScalaType
final case class WithType(types: List[ScalaType]) extends ScalaType
final case class StructuralType(tpe: ScalaType, declarations: List[SymbolInfo]) extends ScalaType
final case class AnnotatedType(annotations: List[Annotation], tpe: ScalaType) extends ScalaType
final case class ExistentialType(tpe: ScalaType, declarations: List[SymbolInfo]) extends ScalaType
final case class UniversalType(typeParameters: List[SymbolInfo], tpe: ScalaType) extends ScalaType
final case class ByNameType(tpe: ScalaType) extends ScalaType
final case class RepeatedType(tpe: ScalaType) extends ScalaType
case object NoType extends ScalaType
