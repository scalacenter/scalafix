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

import test.organizeImports.ExpandUnmodeledM._

object ExpandWildcardUnmodeledReceiver {
  val a = direct
  val b = makeBase().inherited
}
