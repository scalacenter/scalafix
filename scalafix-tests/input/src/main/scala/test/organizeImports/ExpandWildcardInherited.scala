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

import test.organizeImports.ExpandInheritance.Sub._

object ExpandWildcardInherited {
  val a = own
  val b = inherited
}
