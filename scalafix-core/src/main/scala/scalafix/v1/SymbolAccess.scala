package scalafix.v1

sealed abstract class SymbolAccess extends Product with Serializable
case object PrivateAccess extends SymbolAccess
case object PrivateThisAccess extends SymbolAccess
final case class PrivateWithinAccess(sym: Symbol) extends SymbolAccess
case object ProtectedAccess extends SymbolAccess
case object ProtectedThisAccess extends SymbolAccess
final case class ProtectedWithinAccess(sym: Symbol) extends SymbolAccess
case object PublicAccess extends SymbolAccess
case object NoAccess extends SymbolAccess
