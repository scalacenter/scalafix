package scalafix.internal

import scalafix.v0.{Flags => d}
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb.SymbolInformation.{Kind => k}
import scalafix.v0.Denotation

package object v0 {

  implicit class XtensionDenotation(ddenot: Denotation) {
    def dtest(bit: Long) = (ddenot.flags & bit) == bit
    def sproperties: Int = {
      var sproperties = 0
      def sflip(sprop: s.SymbolInformation.Property) =
        sproperties ^= sprop.value
      if (dtest(d.ABSTRACT)) sflip(p.ABSTRACT)
      if (dtest(d.FINAL)) sflip(p.FINAL)
      if (dtest(d.SEALED)) sflip(p.SEALED)
      if (dtest(d.IMPLICIT)) sflip(p.IMPLICIT)
      if (dtest(d.LAZY)) sflip(p.LAZY)
      if (dtest(d.CASE)) sflip(p.CASE)
      if (dtest(d.COVARIANT)) sflip(p.COVARIANT)
      if (dtest(d.CONTRAVARIANT)) sflip(p.CONTRAVARIANT)
      if (dtest(d.VAL)) sflip(p.VAL)
      if (dtest(d.VAR)) sflip(p.VAR)
      if (dtest(d.STATIC)) sflip(p.STATIC)
      if (dtest(d.PRIMARY)) sflip(p.PRIMARY)
      if (dtest(d.ENUM)) sflip(p.ENUM)
      sproperties
    }
    def skind: s.SymbolInformation.Kind = {
      if (dtest(d.LOCAL)) k.LOCAL
      else if (dtest(d.FIELD)) k.FIELD
      else if (dtest(d.METHOD)) k.METHOD
      else if (dtest(d.CTOR)) k.CONSTRUCTOR
      else if (dtest(d.MACRO)) k.MACRO
      else if (dtest(d.TYPE)) k.TYPE
      else if (dtest(d.PARAM)) k.PARAMETER
      else if (dtest(d.SELFPARAM)) k.SELF_PARAMETER
      else if (dtest(d.TYPEPARAM)) k.TYPE_PARAMETER
      else if (dtest(d.OBJECT)) k.OBJECT
      else if (dtest(d.PACKAGE)) k.PACKAGE
      else if (dtest(d.PACKAGEOBJECT)) k.PACKAGE_OBJECT
      else if (dtest(d.CLASS)) k.CLASS
      else if (dtest(d.TRAIT)) k.TRAIT
      else if (dtest(d.INTERFACE)) k.INTERFACE
      else k.UNKNOWN_KIND
    }
  }

}
