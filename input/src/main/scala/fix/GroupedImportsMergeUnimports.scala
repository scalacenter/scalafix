/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package fix

import fix.MergeImports.Unimport1.{a => _, _}
import fix.MergeImports.Unimport1.{b => B}
import fix.MergeImports.Unimport1.{c => _, _}
import fix.MergeImports.Unimport1.d

import fix.MergeImports.Unimport2.{a => _}
import fix.MergeImports.Unimport2.{b => _}
import fix.MergeImports.Unimport2.{c => C}
import fix.MergeImports.Unimport2.d

object GroupedImportsMergeUnimports
