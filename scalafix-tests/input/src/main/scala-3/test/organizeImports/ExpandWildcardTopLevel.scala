/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Scala3
}
 */
package test.organizeImports

import test.organizeImports.wildcards3.*

object ExpandWildcardTopLevel {
  val x1 = Apple
  val x2 = topLevelDef + topLevelVal
}
