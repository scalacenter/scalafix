/*
rules = [OrganizeImports]
OrganizeImports {
  coalesceToWildcardImportThreshold = null
  preset = INTELLIJ_2020_3
  targetDialect = Scala3
  removeUnused = false
}
 */
package test.organizeImports

// Line 14 equivalent: wildcard import from same package as multi-line renames
import scala.collection.immutable._
// Multi-line renames from same package: already sorted, should preserve format
import scala.collection.immutable.{
  ArraySeq as ASeq,
  HashMap as HMap,
  TreeMap as TMap,
}

object MultiLineImportsWithAliasesAndWildcard
