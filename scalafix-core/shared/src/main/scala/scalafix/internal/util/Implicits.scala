package scalafix.internal.util

import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.{semanticdb3 => s}

private[scalafix] object Implicits {
  implicit class XtensionSymbolInformationProperties(
      info: s.SymbolInformation) {
    def typ: s.Type =
      info.tpe.getOrElse(throw new IllegalArgumentException(info.toProtoString))
    def is(property: s.SymbolInformation.Property): Boolean =
      (info.properties & property.value) != 0
    def isVal: Boolean = is(p.VAL)
    def isVar: Boolean = is(p.VAR)
    def isVarSetter: Boolean =
      isVar && info.name.endsWith("_=")
  }
}
