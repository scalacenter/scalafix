package test.organizeImports

// `export` is a plain member name here, not a qualifier; merging two imports
// from the same prefix into one importer must not drop its backticks.
import test.organizeImports.QuotedIdent.{
  ea,
  `export`
}

object MergeScala3KeywordImportee
