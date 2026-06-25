/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Merge
  targetDialect = Scala3
  removeUnused = false
}
 */
package test.organizeImports

// Imports are in WRONG order, mixing aliased and plain names - rule must reorder
import scala.collection.immutable.{
  TreeMap as TMap,
  Vector,
  HashMap as HMap,
  Queue,
  ArraySeq as ASeq,
}

object MultiLineImportsWithAliasesReorder
