/*
rules = OrganizeImports
OrganizeImports.groupedImports = Merge
OrganizeImports.importSelectorsOrder = Ascii
 */
package fix

import fix.MergeImports.Unimport.{a => _, _}
import fix.MergeImports.Unimport.{b => B}
import fix.MergeImports.Unimport.{c => _, _}
import fix.MergeImports.Unimport.d

object GroupedImportsMergeUnimports
