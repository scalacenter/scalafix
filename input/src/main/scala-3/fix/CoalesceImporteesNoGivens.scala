/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
}
 */
package fix

import fix.Givens.{A, B, C => C1}

object CoalesceImporteesNoGivens
