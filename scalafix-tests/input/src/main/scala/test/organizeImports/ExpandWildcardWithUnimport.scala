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

import test.organizeImports.MergeImports.Wildcard1.{a => _, _}

object ExpandWildcardWithUnimport {
  val x = b
}
