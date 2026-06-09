/*
rules = [OrganizeImports]
OrganizeImports {
  groupSeparately = [ByTypeGivens]
  groupedImports = AggressiveMerge
  removeUnused = false
}
 */
package test.organizeImports

import ZGivens.AA
import test.organizeImports.ZGivens.EE
import test.organizeImports.Givens.{A, B}
import test.organizeImports.Givens.{given A, given B, given AA[A], given EE[B]}

object RelativeGenericGivens
