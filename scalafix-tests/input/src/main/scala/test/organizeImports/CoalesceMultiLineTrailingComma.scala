/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  removeUnused = false
  coalesceToWildcardImportThreshold = 2
}
 */
package test.organizeImports

// Coalescing rewrites the importees, so the import no longer counts as
// already-organized: it is pretty-printed (re-flowed to one line) rather than
// re-emitted verbatim, and the formatter re-wraps it afterwards.
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
