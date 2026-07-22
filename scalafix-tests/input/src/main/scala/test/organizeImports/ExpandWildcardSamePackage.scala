/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports.samepkg

import test.organizeImports.samepkg._

object LocalOnly

object ExpandWildcardSamePackage {
  val x = SharedMember
}
