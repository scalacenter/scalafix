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

import test.organizeImports.Givens._
import test.organizeImports.Givens.{A, B => B1, C => _, given D, _}

object CoalesceImporteesGivensAndNames
