/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  removeUnused = false
}
 */
package test.organizeImports

// A wrapped single importee without a trailing comma keeps its layout, and
// does not gain a comma.
import scala.collection.{
  immutable => imm
}

object SingleImporteeWrappedNoComma {
  val m: imm.Map[Int, Int] = imm.Map.empty
}
