/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  removeUnused = false
  coalesceToWildcardImportThreshold = 2
}
 */
package test.organizeImports

// Coalescing rewrites the importees, but the wrapping and the trailing comma
// of the source import are kept.
import scala.collection.immutable.{
  Map => M,
  Set,
  Seq,
}

object CoalesceMultiLineTrailingComma {
  val m: M[Int, Int] = M.empty
  val s: Set[Int] = Set.empty
  val q: Seq[Int] = Seq.empty
}
