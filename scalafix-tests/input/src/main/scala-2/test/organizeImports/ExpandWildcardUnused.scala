/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Scala2
}
 */
package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1._

object ExpandWildcardUnused {
  val x = 1
}
