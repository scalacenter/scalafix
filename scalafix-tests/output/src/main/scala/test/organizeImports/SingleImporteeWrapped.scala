package test.organizeImports

// A wrapped single importee keeps its multi-line layout.
import scala.collection.{
  immutable => imm,
}

object SingleImporteeWrapped {
  val m: imm.Map[Int, Int] = imm.Map.empty
}
