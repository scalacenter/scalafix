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

import test.organizeImports.MergeImports.Wildcard1.{a, _}

object ExpandWildcardWithExplicit {
  val x1 = a
  val x2 = b
}
