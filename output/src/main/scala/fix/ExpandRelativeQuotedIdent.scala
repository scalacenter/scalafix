package fix

import fix.ExpandRelativeQuotedIdent.`a.b`
import fix.ExpandRelativeQuotedIdent.`a.b`.c

object ExpandRelativeQuotedIdent {
  object `a.b` {
    object c
  }
}
