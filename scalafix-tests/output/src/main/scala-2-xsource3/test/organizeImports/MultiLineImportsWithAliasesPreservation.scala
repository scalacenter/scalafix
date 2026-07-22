package test.organizeImports

// Already sorted and already merged: the rule should not collapse the multi-line format
import scala.collection.immutable.{
  HashMap as HMap,
  ListMap as LMap,
  TreeMap as TMap,
}

object MultiLineImportsWithAliasesPreservation
