/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import test.organizeImports.QuotedIdent._

object ExpandWildcardQuotedIdent {
  val x = `macro`
  val y = `a.b`
}
