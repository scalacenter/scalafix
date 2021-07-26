/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  coalesceToWildcardImportThreshold = 2
  removeUnused = false
}
 */
package fix

import fix.GivenImports.{Alpha, Beta, Zeta}
import fix.GivenImports.{given Alpha, given Beta, given Zeta}

object CoalesceGivenImportees
