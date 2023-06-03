/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
}
 */
package test.organizeImports

import test.organizeImports.Givens._
import test.organizeImports.Givens.{A => A1, given B, given C}

object CoalesceImporteesNoNames
