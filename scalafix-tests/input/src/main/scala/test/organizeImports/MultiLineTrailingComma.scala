/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Merge
  removeUnused = false
}
 */
package test.organizeImports

// The source importer ends with a trailing comma: it must be preserved after reordering.
import scala.collection.immutable.{
  TreeMap,
  HashMap,
  BitSet,
}

object MultiLineTrailingComma
