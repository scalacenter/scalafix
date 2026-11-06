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

import MergeImports.Wildcard1._

object ExpandWildcardRelative {
  val x1 = a
}
