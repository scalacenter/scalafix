/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = AggressiveMerge
 */
package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1._
import test.organizeImports.MergeImports.Wildcard1.{a => _, _}
import test.organizeImports.MergeImports.Wildcard1.{b => B}
import test.organizeImports.MergeImports.Wildcard1.{c => _, _}
import test.organizeImports.MergeImports.Wildcard1.d

import test.organizeImports.GivenImports._
import test.organizeImports.GivenImports.{Alpha, Beta, Zeta}
import test.organizeImports.GivenImports.{given Alpha, given Beta, given Zeta}
import test.organizeImports.GivenImports.given

import test.organizeImports.MergeImports.Wildcard2._
import test.organizeImports.MergeImports.Wildcard2.{a, b}


object GroupedImportsAggressiveMergeGivenAll
