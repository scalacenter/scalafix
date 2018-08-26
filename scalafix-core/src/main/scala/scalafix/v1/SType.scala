package scalafix.v1

import scala.runtime.Statics

sealed abstract class SType

case object NoTpe extends SType

final class TypeRef(
    val prefix: SType,
    val symbol: Symbol,
    val typeArguments: List[SType]
) extends SType {
  override def toString: String =
    s"TypeRef($prefix,$symbol,$typeArguments)"
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

final class SingleType(
    val prefix: SType,
    val symbol: Symbol
) extends SType {
  override def toString: String =
    s"SingleType($prefix,$symbol)"
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

final class ThisType(
    val symbol: Symbol
) extends SType {
  override def toString: String =
    s"ThisType($symbol)"
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

final class SuperType(
    val prefix: SType,
    val symbol: Symbol
) extends SType {
  override def toString: String =
    s"SuperType($prefix,$symbol)"
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

final class ConstantType(
    val constant: Constant
) extends SType {
  override def toString: String =
    s"ConstantType($constant)"
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

final class IntersectionType(
    val types: List[SType]
) extends SType {
  override def toString: String =
    s"IntersectionType($types)"
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

final class UnionType(
    val types: List[SType]
) extends SType {
  override def toString: String =
    s"UnionType($types)"
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

final class WithType(
    val types: List[SType]
) extends SType {
  override def toString: String =
    s"WithType($types)"
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

final class StructuralType(
    val tpe: SType,
    val declarations: List[SymbolInfo]
) extends SType {
  override def toString: String =
    s"StructuralType($tpe,$declarations)"
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

final class AnnotatedType(
    val annotations: List[Annotation],
    val tpe: SType
) extends SType {
  override def toString: String =
    s"AnnotatedType($annotations,$tpe)"
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

final class ExistentialType(
    val tpe: SType,
    val declarations: List[SymbolInfo]
) extends SType {
  override def toString: String =
    s"ExistentialType($tpe,$declarations)"
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

final class UniversalType(
    val typeParameters: List[SymbolInfo],
    val tpe: SType
) extends SType {
  override def toString: String =
    s"UniversalType($tpe,$typeParameters)"
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

final class ByNameType(
    val tpe: SType
) extends SType {
  override def toString: String =
    s"ByNameType($tpe)"
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

final class RepeatedType(
    val tpe: SType
) extends SType {
  override def toString: String =
    s"RepeatedType($tpe)"
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
