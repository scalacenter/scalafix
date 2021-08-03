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
import fix.Givens.{A, B => B1, C => _, given D, _}

object CoalesceImporteesGivensAndNames
