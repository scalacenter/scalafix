package scalafix.internal.v1

import scala.meta.internal.{semanticdb => s}
import scalafix.v1._

object FromProtobuf {
  def access(a: s.Access): SymbolAccess = a match {
    case s.NoAccess =>
      NoAccess
    case s.PrivateAccess() =>
      PrivateAccess
    case s.PrivateThisAccess() =>
      PrivateThisAccess
    case s.PrivateWithinAccess(sym) =>
      PrivateWithinAccess(Symbol(sym))
    case s.ProtectedAccess() =>
      ProtectedAccess
    case s.ProtectedThisAccess() =>
      ProtectedThisAccess
    case s.ProtectedWithinAccess(sym) =>
      ProtectedWithinAccess(Symbol(sym))
    case s.PublicAccess() =>
      PublicAccess
  }

}
