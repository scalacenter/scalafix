package scalafix.v1

import scala.runtime.Statics

final class Annotation(val tpe: SType) {
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
