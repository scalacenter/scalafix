/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package fix

import fix.GivenImports.Beta
import fix.GivenImports.Alpha
import fix.GivenImports.{given Beta}
import fix.GivenImports.{given Alpha}
import scala.util.Either

object MergeGiven
