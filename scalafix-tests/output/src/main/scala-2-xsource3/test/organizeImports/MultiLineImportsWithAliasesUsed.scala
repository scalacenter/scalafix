package test.organizeImports

// Already sorted and already merged, imports are USED: the rule should not collapse the multi-line format
import scala.collection.immutable.{
  HashMap as HMap,
  ListMap as LMap,
  TreeMap as TMap,
}

object MultiLineImportsWithAliasesUsed {
  val a: LMap[String, Int] = LMap("a" -> 1)
  val b: HMap[String, Int] = HMap("x" -> 1)
  val c: TMap[String, Int] = TMap("y" -> 2)
}
