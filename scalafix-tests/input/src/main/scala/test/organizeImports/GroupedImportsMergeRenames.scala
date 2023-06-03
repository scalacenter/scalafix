/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package test.organizeImports

import test.organizeImports.MergeImports.Rename1.{a => A}
import test.organizeImports.MergeImports.Rename1.{b => B}
import test.organizeImports.MergeImports.Rename1.c
import test.organizeImports.MergeImports.Rename1.d

import test.organizeImports.MergeImports.Rename2.a
import test.organizeImports.MergeImports.Rename2.{a => A}
import test.organizeImports.MergeImports.Rename2.b
import test.organizeImports.MergeImports.Rename2.{b => B}
import test.organizeImports.MergeImports.Rename2.c

object GroupedImportsMergeRenames
