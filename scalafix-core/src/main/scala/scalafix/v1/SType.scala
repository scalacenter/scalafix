package scalafix.v1

import scala.runtime.Statics
import scalafix.internal.util.Pretty
import scalafix.util.FieldNames

sealed abstract class SType extends Product with FieldNames {
  final override def toString: String = Pretty.pretty(this).render(80)
  final def isEmpty: Boolean = this == NoType
  final def nonEmpty: Boolean = !isEmpty
}

object Types {
  def scalaNothing: TypeRef =
    new TypeRef(NoType, Symbol("scala/Nothing#"), Nil)
  def scalaAny: TypeRef =
    new TypeRef(NoType, Symbol("scala/Any#"), Nil)
  def javaObject: TypeRef =
    new TypeRef(NoType, Symbol("java/lang/Object#"), Nil)
}

case object NoType extends SType

final class TypeRef private[scalafix] (
    val prefix: SType,
    val symbol: Symbol,
    val typeArguments: List[SType]
) extends SType {
  override def productArity: Int = 3
  override def productPrefix: String = "TypeRef"
  override def productElement(n: Int): Any = n match {
    case 0 => prefix
    case 1 => symbol
    case 2 => typeArguments
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "prefix"
    case 1 => "symbol"
    case 2 => "typeArguments"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[TypeRef]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TypeRef =>
        this.prefix == s.prefix &&
          this.symbol == s.symbol &&
          this.typeArguments == s.typeArguments
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(prefix))
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    acc = Statics.mix(acc, Statics.anyHash(typeArguments))
    Statics.finalizeHash(acc, 3)
  }
}

final class SingleType private[scalafix] (
    val prefix: SType,
    val symbol: Symbol
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "SingleType"
  override def productElement(n: Int): Any = n match {
    case 0 => prefix
    case 1 => symbol
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "prefix"
    case 1 => "symbol"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[SingleType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TypeRef =>
        this.prefix == s.prefix &&
          this.symbol == s.symbol
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(prefix))
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    Statics.finalizeHash(acc, 2)
  }
}

final class ThisType private[scalafix] (
    val symbol: Symbol
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "ThisType"
  override def productElement(n: Int): Any = n match {
    case 0 => symbol
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "symbol"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ThisType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ThisType =>
        this.symbol == s.symbol
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    Statics.finalizeHash(acc, 1)
  }
}

final class SuperType private[scalafix] (
    val prefix: SType,
    val symbol: Symbol
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "SuperType"
  override def productElement(n: Int): Any = n match {
    case 0 => prefix
    case 1 => symbol
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "prefix"
    case 1 => "symbol"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[SuperType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: SuperType =>
        this.prefix == s.prefix &&
          this.symbol == s.symbol
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(prefix))
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    Statics.finalizeHash(acc, 2)
  }
}

final class ConstantType private[scalafix] (
    val constant: Constant
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "ConstantType"
  override def productElement(n: Int): Any = n match {
    case 0 => constant
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "constant"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ConstantType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ConstantType =>
        this.constant == s.constant
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(constant))
    Statics.finalizeHash(acc, 1)
  }
}

final class IntersectionType private[scalafix] (
    val types: List[SType]
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "IntersectionType"
  override def productElement(n: Int): Any = n match {
    case 0 => types
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "types"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[IntersectionType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: IntersectionType =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class UnionType private[scalafix] (
    val types: List[SType]
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "UnionType"
  override def productElement(n: Int): Any = n match {
    case 0 => types
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "types"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[UnionType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: UnionType =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class WithType private[scalafix] (
    val types: List[SType]
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "WithType"
  override def productElement(n: Int): Any = n match {
    case 0 => types
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "types"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[WithType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: WithType =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class StructuralType private[scalafix] (
    val tpe: SType,
    val declarations: List[SymbolInfo]
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "StructuralType"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case 1 => declarations
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "tpe"
    case 1 => "declarations"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[StructuralType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: StructuralType =>
        this.declarations == s.declarations &&
          this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(declarations))
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 2)
  }
}

final class AnnotatedType private[scalafix] (
    val annotations: List[Annotation],
    val tpe: SType
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "AnnotatedType"
  override def productElement(n: Int): Any = n match {
    case 0 => annotations
    case 1 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "annotations"
    case 1 => "tpe"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[AnnotatedType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: AnnotatedType =>
        this.annotations == s.annotations &&
          this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(annotations))
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 2)
  }
}

final class ExistentialType private[scalafix] (
    val tpe: SType,
    val declarations: List[SymbolInfo]
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "ExistentialType"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case 1 => declarations
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "tpe"
    case 1 => "declarations"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ExistentialType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ExistentialType =>
        this.declarations == s.declarations &&
          this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(declarations))
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 2)
  }
}

final class UniversalType private[scalafix] (
    val typeParameters: List[SymbolInfo],
    val tpe: SType
) extends SType {
  override def productArity: Int = 2
  override def productPrefix: String = "UniversalType"
  override def productElement(n: Int): Any = n match {
    case 0 => typeParameters
    case 1 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "typeParameters"
    case 1 => "tpe"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[UniversalType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: UniversalType =>
        this.typeParameters == s.typeParameters &&
          this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(typeParameters))
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 2)
  }
}

final class ByNameType private[scalafix] (
    val tpe: SType
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "ByNameType"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "tpe"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[ByNameType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ByNameType =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}

final class RepeatedType private[scalafix] (
    val tpe: SType
) extends SType {
  override def productArity: Int = 1
  override def productPrefix: String = "RepeatedType"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def fieldName(n: Int): String = n match {
    case 0 => "tpe"
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[RepeatedType]
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ByNameType =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}
