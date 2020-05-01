/*
rules = OrganizeImports
OrganizeImports.groupedImports = Merge
OrganizeImports.importSelectorsOrder = Ascii
 */
package fix

import fix.MergeImports.Rename1.{a => A}
import fix.MergeImports.Rename1.{b => B}
import fix.MergeImports.Rename1.c
import fix.MergeImports.Rename1.d

import fix.MergeImports.Rename2.a
import fix.MergeImports.Rename2.{a => A}
import fix.MergeImports.Rename2.b
import fix.MergeImports.Rename2.{b => B}
import fix.MergeImports.Rename2.c

object GroupedImportsMergeRenames
