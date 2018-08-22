package scalafix.v1

import scala.collection.mutable.ListBuffer
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}

final class SymbolProperties private[scalafix] (
    private[scalafix] val props: Int
) {
  def isAbstract: Boolean = is(p.ABSTRACT)
  def isFinal: Boolean = is(p.FINAL)
  def isSealed: Boolean = is(p.SEALED)
  def isImplicit: Boolean = is(p.IMPLICIT)
  def isLazy: Boolean = is(p.LAZY)
  def isCase: Boolean = is(p.CASE)
  def isCovariant: Boolean = is(p.COVARIANT)
  def isContravariant: Boolean = is(p.CONTRAVARIANT)
  def isStatic: Boolean = is(p.STATIC)
  def isPrimary: Boolean = is(p.PRIMARY)
  def isEnum: Boolean = is(p.ENUM)
  def isVal: Boolean = is(p.VAL)
  def isVar: Boolean = is(p.VAR)

  // privates
  private[this] def is(property: s.SymbolInformation.Property): Boolean =
    (props & property.value) != 0

  override def toString: String = {
    val buf = ListBuffer.empty[String]
    if (isAbstract) buf += "abstract"
    if (isFinal) buf += "final"
    if (isSealed) buf += "sealed"
    if (isImplicit) buf += "implicit"
    if (isLazy) buf += "lazy"
    if (isCase) buf += "case"
    if (isContravariant) buf += "covariant"
    if (isContravariant) buf += "contravariant"
    if (isStatic) buf += "static"
    if (isPrimary) buf += "primary"
    if (isEnum) buf += "enum"
    if (isVal) buf += "val"
    if (isVar) buf += "var"
    buf.mkString("SymbolProperties(", " ", ")")
  }
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: SymbolProperties => this.props == s.props
      case _ => false
    })
  override def hashCode(): Int = props.##
}
