/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = Scala3
OrganizeImports.groups = ["*"]
 */
package test.organizeImports

import test.organizeImports.GivenImports.Alpha
import test.organizeImports.Givens.{given Alpha}

object GivenImportsCrossPackage
