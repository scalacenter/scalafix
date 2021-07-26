/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package fix

import fix.GivenImports.Beta
import fix.GivenImports.Alpha
import fix.GivenImports.{given Beta, given Alpha}
import scala.util.Either

object ExpandGiven
