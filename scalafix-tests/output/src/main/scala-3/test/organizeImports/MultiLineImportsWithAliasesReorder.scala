package test.organizeImports

// Imports are in WRONG order: TreeMap before HashMap - rule must reorder and will lose multi-line format
import scala.collection.immutable.{ArraySeq as ASeq, HashMap as HMap, TreeMap as TMap}

object MultiLineImportsWithAliasesReorder
