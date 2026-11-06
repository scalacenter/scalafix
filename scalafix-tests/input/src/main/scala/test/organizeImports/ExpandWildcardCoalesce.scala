/*
rules = [OrganizeImports]
OrganizeImports {
  coalesceToWildcardImportThreshold = 4
  expandWildcardImportThreshold = 4
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1._

object ExpandWildcardCoalesce {
  val x1 = a
  val x2 = b
  val x3 = c
}
