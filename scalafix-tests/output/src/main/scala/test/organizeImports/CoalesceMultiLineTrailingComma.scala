package test.organizeImports

// Coalescing rewrites the importees, but the wrapping and the trailing comma
// of the source import are kept.
import scala.collection.immutable.{
  Map => M,
  _,
}

object CoalesceMultiLineTrailingComma {
  val m: M[Int, Int] = M.empty
  val s: Set[Int] = Set.empty
  val q: Seq[Int] = Seq.empty
}
