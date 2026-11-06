package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1.*
import test.organizeImports.MergeImports.Wildcard2.a

object ExpandWildcardQualified {
  // `a` is referenced from Wildcard1 only fully-qualified, so the wildcard is
  // not expanded to `Wildcard1.a` (which would clash with the explicit
  // `Wildcard2.a` and make the unqualified `a` below ambiguous).
  val x = MergeImports.Wildcard1.a
  val y = a
}
