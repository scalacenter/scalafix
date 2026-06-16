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

import test.organizeImports.InstanceCall.Sub._
import test.organizeImports.InstanceCall.other

object ExpandWildcardInstanceCall {
  // `own` is used unqualified and drives the expansion; `other.inherited` is an
  // ordinary inherited instance call, so even though `inherited` is owned by
  // `Base` (which `Sub` also extends) it must NOT be added as `Sub.inherited`.
  val a = own
  val b = other.inherited
}
