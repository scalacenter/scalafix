package test.organizeImports

// Line 14 equivalent: wildcard import from same package as multi-line renames
import scala.collection.immutable.*
import scala.collection.immutable.{
  ArraySeq as ASeq,
  HashMap as HMap,
  TreeMap as TMap
}

object MultiLineImportsWithAliasesAndWildcard
