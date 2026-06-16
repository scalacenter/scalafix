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

import test.organizeImports.pkgobj._

object ExpandWildcardPackageObject {
  val a = directMember
  val b = inheritedMember
}
