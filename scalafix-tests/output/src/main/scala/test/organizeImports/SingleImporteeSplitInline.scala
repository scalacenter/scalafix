package test.organizeImports

// Importees split out of a wrapped multi-importee import are printed inline:
// the wrapping belonged to the source import, not to each importee.
import scala.collection.immutable.Set
import scala.collection.immutable.{Map => M}

object SingleImporteeSplitInline {
  val m: M[Int, Int] = M.empty
  val s: Set[Int] = Set.empty
}
