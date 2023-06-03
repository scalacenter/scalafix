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

import test.organizeImports.MergeImports.Wildcard2._
import test.organizeImports.MergeImports.Wildcard2.{a, b}

object GroupedImportsAggressiveMergeWildcard
