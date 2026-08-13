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

import test.organizeImports.MergeImports.Wildcard1.{a => a1, _}

object ExpandWildcardRename {
  val x1 = a1
  val x2 = b
}
