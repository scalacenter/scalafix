package test.organizeImports

// Imports are in WRONG order, mixing aliased and plain names - rule must reorder
import scala.collection.immutable.{
  ArraySeq as ASeq,
  HashMap as HMap,
  Queue,
  TreeMap as TMap,
  Vector,
}

object MultiLineImportsWithAliasesReorder
