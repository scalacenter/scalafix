package scalafix.v1

import scala.runtime.Statics
import scalafix.internal.util.Pretty
import scalafix.util.FieldNames

sealed abstract class Signature extends Product with FieldNames {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoSignature
  final def nonEmpty: Boolean = !isEmpty
}

case object NoSignature extends Signature

final class ClassSignature private[scalafix] (
    val typeParameters: List[SymbolInfo],
    val parents: List[SType],
    val self: SType,
    val declarations: List[SymbolInfo]
) extends Signature {
  override def productArity: Int = 4
  override def productPrefix: String = "ClassSignature"
  override def productElement(n: Int): Any = n match {
    case 0 => typeParameters
    case 1 => parents
    case 2 => self
    case 3 => declarations
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "typeParameters"
    case 1 => "parents"
    case 2 => "self"
    case 3 => "declarations"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean = that.isInstanceOf[ClassSignature]
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

final class MethodSignature private[scalafix] (
    val typeParameters: List[SymbolInfo],
    val parameterLists: List[List[SymbolInfo]],
    val returnType: SType
) extends Signature {
  override def productArity: Int = 3
  override def productPrefix: String = "MethodSignature"
  override def productElement(n: Int): Any = n match {
    case 0 => typeParameters
    case 1 => parameterLists
    case 2 => returnType
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "typeParameters"
    case 1 => "parameterLists"
    case 2 => "returnType"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean = that.isInstanceOf[MethodSignature]
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

final class TypeSignature private[scalafix] (
    val typeParameters: List[SymbolInfo],
    val lowerBound: SType,
    val upperBound: SType
) extends Signature {
  override def productArity: Int = 3
  override def productPrefix: String = "TypeSignature"
  override def productElement(n: Int): Any = n match {
    case 0 => typeParameters
    case 1 => lowerBound
    case 2 => upperBound
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "typeParameters"
    case 1 => "lowerBound"
    case 2 => "upperBound"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean = that.isInstanceOf[TypeSignature]
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

final class ValueSignature private[scalafix] (
    val tpe: SType
) extends Signature {
  override def productArity: Int = 1
  override def productPrefix: String = "ValueSignature"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "tpe"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean = that.isInstanceOf[ValueSignature]
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
