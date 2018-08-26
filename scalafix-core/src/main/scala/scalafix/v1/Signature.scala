package scalafix.v1

import scala.runtime.Statics

sealed abstract class Signature

case object NoSignature extends Signature

final class ClassSignature(
    val typeParameters: List[SymbolInfo],
    val parents: List[SType],
    val self: SType,
    val declarations: List[SymbolInfo]
) extends Signature {
  override def toString: String =
    s"ClassSignature($typeParameters,$parents,$self,$declarations)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ClassSignature =>
        this.typeParameters == s.typeParameters &&
          this.parents == s.parents &&
          this.self == s.self
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(typeParameters))
    acc = Statics.mix(acc, Statics.anyHash(parents))
    acc = Statics.mix(acc, Statics.anyHash(self))
    acc = Statics.mix(acc, Statics.anyHash(declarations))
    Statics.finalizeHash(acc, 4)
  }
}

final class MethodSignature(
    val typeParameters: List[SymbolInfo],
    val parameterLists: List[List[SymbolInfo]],
    val returnType: SType
) extends Signature {
  override def toString: String =
    s"MethodSignature($typeParameters,$parameterLists,$returnType)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: MethodSignature =>
        this.typeParameters == s.typeParameters &&
          this.returnType == s.returnType
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(typeParameters))
    acc = Statics.mix(acc, Statics.anyHash(parameterLists))
    acc = Statics.mix(acc, Statics.anyHash(returnType))
    Statics.finalizeHash(acc, 3)
  }
}

final class TypeSignature(
    val typeParameters: List[SymbolInfo],
    val lowerBound: SType,
    val upperBound: SType
) extends Signature {
  override def toString: String =
    s"TypeSignature($typeParameters,$lowerBound,$upperBound)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TypeSignature =>
        this.typeParameters == s.typeParameters &&
          this.lowerBound == s.lowerBound &&
          this.upperBound == s.upperBound
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(typeParameters))
    acc = Statics.mix(acc, Statics.anyHash(lowerBound))
    acc = Statics.mix(acc, Statics.anyHash(upperBound))
    Statics.finalizeHash(acc, 3)
  }
}

final class ValueSignature(
    val tpe: SType
) extends Signature {
  override def toString: String =
    s"ValueSignature($tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ValueSignature =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}
