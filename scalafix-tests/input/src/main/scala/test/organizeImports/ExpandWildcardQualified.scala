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

import test.organizeImports.MergeImports.Wildcard1._
import test.organizeImports.MergeImports.Wildcard2.a

object ExpandWildcardQualified {
  // `a` is referenced from Wildcard1 only fully-qualified, so the wildcard is
  // not expanded to `Wildcard1.a` (which would clash with the explicit
  // `Wildcard2.a` and make the unqualified `a` below ambiguous).
  val x = MergeImports.Wildcard1.a
  val y = a
}
