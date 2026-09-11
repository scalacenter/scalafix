/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupExplicitlyImportedImplicitsSeparately = true
  groupedImports = Keep
  removeUnused = false
  targetDialect = Auto
}
 */
package test.organizeImports

import test.organizeImports.Implicits.a._
import test.organizeImports.MergeImports.Wildcard1.a

object ExpandWildcardImplicitSeparate {
  def needsInt(implicit i: Int): Int = i
  val x: Int = needsInt
  val y = a
}
