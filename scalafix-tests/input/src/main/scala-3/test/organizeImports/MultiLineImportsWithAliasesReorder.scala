/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Merge
  targetDialect = Scala3
  removeUnused = false
}
 */
package test.organizeImports

// Imports are in WRONG order: TreeMap before HashMap - rule must reorder and will lose multi-line format
import scala.collection.immutable.{
  TreeMap as TMap,
  HashMap as HMap,
  ArraySeq as ASeq,
}

object MultiLineImportsWithAliasesReorder
