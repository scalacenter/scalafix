package test.organizeImports

// The multi-line import is split into two statements; Merge combines them
// The result should preserve the merged importees (order: ArraySeq, HashMap, TreeMap)
import scala.collection.immutable.{ArraySeq as ASeq, HashMap as HMap, TreeMap as TMap}

object MultiLineImportsWithAliasesMerge
