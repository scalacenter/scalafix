package scalafix.v1

import scala.runtime.Statics

sealed abstract class Tpe

case object NoTpe extends Tpe

final class TpeRef(
    val prefix: Tpe,
    val symbol: Symbol,
    val typeArguments: List[Tpe]
) extends Tpe {
  override def toString: String =
    s"TypeRef($prefix,$symbol,$typeArguments)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TpeRef =>
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

final class SingleTpe(
    val prefix: Tpe,
    val symbol: Symbol
) extends Tpe {
  override def toString: String =
    s"SingleType($prefix,$symbol)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TpeRef =>
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

final class ThisTpe(
    val symbol: Symbol
) extends Tpe {
  override def toString: String =
    s"ThisType($symbol)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ThisTpe =>
        this.symbol == s.symbol
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    Statics.finalizeHash(acc, 1)
  }
}

final class SuperTpe(
    val prefix: Tpe,
    val symbol: Symbol
) extends Tpe {
  override def toString: String =
    s"SuperType($prefix,$symbol)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: SuperTpe =>
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

final class ConstantTpe(
    val constant: Constant
) extends Tpe {
  override def toString: String =
    s"ConstantType($constant)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ConstantTpe =>
        this.constant == s.constant
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(constant))
    Statics.finalizeHash(acc, 1)
  }
}

final class IntersectionTpe(
    val types: List[Tpe]
) extends Tpe {
  override def toString: String =
    s"IntersectionType($types)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: IntersectionTpe =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class UnionTpe(
    val types: List[Tpe]
) extends Tpe {
  override def toString: String =
    s"UnionType($types)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: UnionTpe =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class WithTpe(
    val types: List[Tpe]
) extends Tpe {
  override def toString: String =
    s"WithType($types)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: WithTpe =>
        this.types == s.types
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(types))
    Statics.finalizeHash(acc, 1)
  }
}

final class StructuralTpe(
    val tpe: Tpe,
    val declarations: List[SymbolInfo]
) extends Tpe {
  override def toString: String =
    s"StructuralType($tpe,$declarations)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: StructuralTpe =>
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

final class AnnotatedTpe(
    val annotations: List[Annotation],
    val tpe: Tpe
) extends Tpe {
  override def toString: String =
    s"AnnotatedType($annotations,$tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: AnnotatedTpe =>
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

final class ExistentialTpe(
    val tpe: Tpe,
    val declarations: List[SymbolInfo]
) extends Tpe {
  override def toString: String =
    s"ExistentialType($tpe,$declarations)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ExistentialTpe =>
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

final class UniversalTpe(
    val typeParameters: List[SymbolInfo],
    val tpe: Tpe
) extends Tpe {
  override def toString: String =
    s"UniversalType($tpe,$typeParameters)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: UniversalTpe =>
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

final class ByNameTpe(
    val tpe: Tpe
) extends Tpe {
  override def toString: String =
    s"ByNameType($tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ByNameTpe =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}

final class RepeatedTpe(
    val tpe: Tpe
) extends Tpe {
  override def toString: String =
    s"RepeatedType($tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ByNameTpe =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}
