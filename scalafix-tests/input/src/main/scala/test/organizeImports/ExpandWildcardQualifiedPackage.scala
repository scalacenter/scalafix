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

import test.organizeImports.qualifiedpkg._

object ExpandWildcardQualifiedPackage {
  val unqualified: QualifiedMember = new QualifiedMember
  val qualified: qualifiedpkg.QualifiedMember = new qualifiedpkg.QualifiedMember
}