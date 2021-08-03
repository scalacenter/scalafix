/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
}
 */
package fix

import fix.Givens._
import fix.Givens.{A => A1, given B, given C}

object CoalesceImporteesNoNames
