/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package test.organizeImports

import test.organizeImports.GivenImports.Beta
import test.organizeImports.GivenImports.Alpha
import test.organizeImports.GivenImports.{given Beta, given Alpha}
import scala.util.Either

object ExpandGiven
