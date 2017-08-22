package scalafix.util

import scala.meta.tokens.Token

object TokenOps {

  /** Returns true if this token is an identifier that requires a leading space before colon.
    *
    * Example:
    *   needsLeadingSpaceBeforeColon(foo_) // true
    *   needsLeadingSpaceBeforeColon(foo)  // false
    *   val foo_ : Int = 2 // OK
    *   val foo_: Int = 2  // ERROR
    *   val foo: Int = 2   // OK
    *
    **/
  def needsLeadingSpaceBeforeColon(ident: Token): Boolean = ident match {
    case Token.Ident(_) =>
      ident.syntax.lastOption.exists {
        case '`' => false
        case ch => !ch.isLetterOrDigit
      }
    case _ => false
  }
}
