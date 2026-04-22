package test.organizeImports

// Already sorted and already merged: the rule should not collapse the multi-line format
import scala.collection.immutable.{
  ArraySeq as ASeq,
  HashMap as HMap,
  TreeMap as TMap
}

object MultiLineImportsWithAliasesPreservation
