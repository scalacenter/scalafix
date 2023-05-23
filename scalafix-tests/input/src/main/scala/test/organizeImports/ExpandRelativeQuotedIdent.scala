/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.expandRelative = true
 */
package test.organizeImports

import QuotedIdent.`a.b`
import QuotedIdent.`macro`
import `a.b`.c
import `a.b`.`{ d }`

object ExpandRelativeQuotedIdent
