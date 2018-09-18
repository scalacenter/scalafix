package scalafix.v1

import scala.runtime.Statics

final class Annotation(tpe: SemanticType) extends Product {
  // NOTE(olafur): This is intentionally not a case class to ease future
  // migrations when annotations gain term arguments.
  override def productArity: Int = 1
  override def productPrefix: String = "Annotation"
  override def productElement(n: Int): Any = n match {
    case 0 => tpe
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }
  override def canEqual(that: Any): Boolean =
    that.isInstanceOf[Annotation]
  override def toString: String = s"Annotation($tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: Annotation =>
        this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 1)
  }
}
