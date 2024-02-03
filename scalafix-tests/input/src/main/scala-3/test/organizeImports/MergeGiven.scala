/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
OrganizeImports.targetDialect = Scala3
 */
package test.organizeImports

import test.organizeImports.GivenImports.Beta
import test.organizeImports.GivenImports.Alpha
import test.organizeImports.GivenImports.given Beta
import test.organizeImports.GivenImports.given test.organizeImports.GivenImports.Gamma
import test.organizeImports.GivenImports.given Alpha
import scala.util.Either

object MergeGiven
