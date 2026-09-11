/*
rules = [OrganizeImports]
OrganizeImports {
  coalesceToWildcardImportThreshold = 2
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1._

object ExpandWildcardCoalesceMisordered {
  val x1 = a
  val x2 = b
  val x3 = c
}
