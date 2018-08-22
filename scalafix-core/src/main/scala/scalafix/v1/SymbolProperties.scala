package scalafix.v1

import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}

final class SymbolProperties private[scalafix] (props: Int) {
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
}
