/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
  targetDialect = Scala3
}
 */
package test.organizeImports

import test.organizeImports.Givens.{A, B, C => C1}

object CoalesceImporteesNoGivens
