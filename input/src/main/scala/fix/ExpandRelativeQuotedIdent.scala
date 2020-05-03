/*
rules = [OrganizeImports]
OrganizeImports.expandRelative = true
 */
package fix

import QuotedIdent.`a.b`
import `a.b`.c

object ExpandRelativeQuotedIdent {
  val refC = c
}
