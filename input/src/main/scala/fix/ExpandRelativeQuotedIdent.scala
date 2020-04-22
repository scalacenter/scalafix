/*
rules = OrganizeImports
OrganizeImports.expandRelative = true
 */

package fix

import ExpandRelativeQuotedIdent.`a.b`
import `a.b`.c

object ExpandRelativeQuotedIdent {
  object `a.b` {
    object c
  }
}
