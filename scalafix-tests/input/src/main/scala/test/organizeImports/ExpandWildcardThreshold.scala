/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 3
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1._

object ExpandWildcardThreshold {
  val x1 = a
  val x2 = b
  val x3 = c
}
