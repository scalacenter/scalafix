/*
rules = [OrganizeImports]
OrganizeImports.groupedImports = Merge
 */
package fix

import fix.MergeImports.Dedup.a
import fix.MergeImports.Dedup.a
import fix.MergeImports.Dedup.{b => b1}
import fix.MergeImports.Dedup.{b => b1}
import fix.MergeImports.Dedup.{c => _}
import fix.MergeImports.Dedup.{c => _}

object GroupedImportsMergeDedup
