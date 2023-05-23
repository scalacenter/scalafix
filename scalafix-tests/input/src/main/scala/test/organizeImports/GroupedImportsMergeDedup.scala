/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package test.organizeImports

import test.organizeImports.MergeImports.Dedup.a
import test.organizeImports.MergeImports.Dedup.a
import test.organizeImports.MergeImports.Dedup.{b => b1}
import test.organizeImports.MergeImports.Dedup.{b => b1}
import test.organizeImports.MergeImports.Dedup.{c => _}
import test.organizeImports.MergeImports.Dedup.{c => _}

object GroupedImportsMergeDedup
