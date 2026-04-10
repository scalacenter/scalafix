/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Merge
  targetDialect = Scala3
  removeUnused = false
}
 */
package test.organizeImports

// The multi-line import is split into two statements; Merge combines them
// The result should preserve the merged importees (order: ArraySeq, HashMap, TreeMap)
import scala.collection.immutable.{
  HashMap as HMap,
  TreeMap as TMap,
}
import scala.collection.immutable.ArraySeq as ASeq

object MultiLineImportsWithAliasesMerge
