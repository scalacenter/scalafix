/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package test.organizeImports

import test.organizeImports.MergeImports.Unimport1.{a => _, _}
import test.organizeImports.MergeImports.Unimport1.{b => B}
import test.organizeImports.MergeImports.Unimport1.{c => _, _}
import test.organizeImports.MergeImports.Unimport1.d

import test.organizeImports.MergeImports.Unimport2.{a => _}
import test.organizeImports.MergeImports.Unimport2.{b => _}
import test.organizeImports.MergeImports.Unimport2.{c => C}
import test.organizeImports.MergeImports.Unimport2.d

object GroupedImportsMergeUnimports
