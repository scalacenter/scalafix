/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
OrganizeImports.targetDialect = Scala3
 */
package test.organizeImports

import test.organizeImports.GivenImports.*
import test.organizeImports.GivenImports.{alpha as _, given}
import test.organizeImports.GivenImports.given Beta
import test.organizeImports.GivenImports.{gamma as _, given}
import test.organizeImports.GivenImports.given Zeta

import test.organizeImports.GivenImports2.alpha as _
import test.organizeImports.GivenImports2.beta as _
import test.organizeImports.GivenImports2.given Gamma
import test.organizeImports.GivenImports2.given Zeta

object GroupedGivenImportsMergeUnimports
