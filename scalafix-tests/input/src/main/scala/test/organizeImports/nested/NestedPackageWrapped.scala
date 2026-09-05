/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  removeUnused = false
}
 */
package test.organizeImports.nested {
  // Wrapped imports inside an indented block keep their relative indentation.
  import scala.collection.immutable.{
    Map => M,
    Set,
  }
  import scala.collection.{
    immutable => imm,
  }

  object NestedPackageWrapped {
    val m: M[Int, Int] = M.empty
    val s: Set[Int] = Set.empty
    val i: imm.Map[Int, Int] = imm.Map.empty
  }
}
