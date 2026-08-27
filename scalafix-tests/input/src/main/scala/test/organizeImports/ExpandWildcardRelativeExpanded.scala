/*
rules = [OrganizeImports]
OrganizeImports {
  expandRelative = true
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import MergeImports.Wildcard1._

object ExpandWildcardRelativeExpanded {
  val x1 = a
}
