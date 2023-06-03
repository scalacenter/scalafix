/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package test.organizeImports

import test.organizeImports.GivenImports._
import test.organizeImports.GivenImports.{alpha => _, given}
import test.organizeImports.GivenImports.{given Beta}
import test.organizeImports.GivenImports.{gamma => _, given}
import test.organizeImports.GivenImports.{given Zeta}

import test.organizeImports.GivenImports2.{alpha => _}
import test.organizeImports.GivenImports2.{beta => _}
import test.organizeImports.GivenImports2.{given Gamma}
import test.organizeImports.GivenImports2.{given Zeta}

object GroupedGivenImportsMergeUnimports
