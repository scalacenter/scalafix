/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = StandardLayout
OrganizeImports.groupedImports = Merge
*/
package test.organizeImports

// `export` is a plain member name here, not a qualifier; merging two imports
// from the same prefix into one importer must not drop its backticks.
import test.organizeImports.QuotedIdent.ea
import test.organizeImports.QuotedIdent.`export`

object MergeScala3KeywordImportee
