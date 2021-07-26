/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package fix

import fix.GivenImports._
import fix.GivenImports.{given Beta, given Alpha}
import fix.GivenImports.given Beta
import fix.GivenImports.given Alpha
import fix.GivenImports.given Alpha

object DeduplicateGivenImportees
