package scalafix.v1

import scala.meta.internal.{semanticdb => s}

final class SymbolAccess private[scalafix] (a: s.Access) {
  def isPrivate: Boolean = a.isInstanceOf[s.PrivateAccess]
  def isPrivateThis: Boolean = a.isInstanceOf[s.PrivateThisAccess]
  def privateWithin: Option[Symbol] = a match {
    case s.PrivateWithinAccess(symbol) => Some(Symbol(symbol))
    case _ => scala.None
  }
  def isProtected: Boolean = a.isInstanceOf[s.ProtectedAccess]
  def isProtectedThis: Boolean = a.isInstanceOf[s.ProtectedThisAccess]
  def protectedWithin: Option[Symbol] = a match {
    case s.ProtectedWithinAccess(symbol) => Some(Symbol(symbol))
    case _ => scala.None
  }
  def isPublic: Boolean = a.isInstanceOf[s.PublicAccess]
  def isNone: Boolean = a == s.NoAccess
}
