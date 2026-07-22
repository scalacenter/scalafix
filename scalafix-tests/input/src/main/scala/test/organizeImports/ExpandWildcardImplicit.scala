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

import test.organizeImports.Implicits.a._

object ExpandWildcardImplicit {
  def needsInt(implicit i: Int): Int = i
  val x: Int = needsInt
}
