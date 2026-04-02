/*
rules = [OrganizeImports]
OrganizeImports {
  coalesceToWildcardImportThreshold = null
  preset = INTELLIJ_2020_3
  targetDialect = Scala3
}
 */
package test.organizeImports

// Already sorted and already merged, imports are USED: the rule should not collapse the multi-line format
import scala.collection.immutable.{
  ArraySeq as ASeq,
  HashMap as HMap,
  TreeMap as TMap,
}

object MultiLineImportsWithAliasesUsed {
  val a: ASeq[Int] = ASeq(1, 2, 3)
  val b: HMap[String, Int] = HMap("x" -> 1)
  val c: TMap[String, Int] = TMap("y" -> 2)
}
