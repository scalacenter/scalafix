/*
rules = [OrganizeImports]
OrganizeImports {
  groupSeparately = [ByTypeGivens]
  groupedImports = AggressiveMerge
  removeUnused = false
}
 */
package test.organizeImports

import test.organizeImports.Givens.{A, B}
import test.organizeImports.AGivens.{given A, given B}

object PreserveDependentGivens
