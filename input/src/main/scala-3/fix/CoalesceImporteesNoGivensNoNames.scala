/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
}
 */
package fix

import fix.Givens.{A => A1, B => _, _}

object CoalesceImporteesNoGivensNoNames
