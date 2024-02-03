/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = Scala3
 */
package test.organizeImports

import test.organizeImports.GivenImports.Beta
import test.organizeImports.GivenImports.Alpha
import test.organizeImports.GivenImports.{given Beta, given test.organizeImports.GivenImports.Gamma, given Alpha}
import scala.util.Either

object ExpandGiven
