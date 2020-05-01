package fix

import fix.QuotedIdent.`a.b`
import fix.QuotedIdent.`a.b`.c

object ExpandRelativeQuotedIdent {
  val refC = c
}
