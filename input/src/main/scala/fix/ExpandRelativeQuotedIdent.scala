/*
rules = [OrganizeImports]
OrganizeImports.expandRelative = true
 */
package fix

import QuotedIdent.`a.b`
import `a.b`.c
import `a.b`.`{ d }`

object ExpandRelativeQuotedIdent
