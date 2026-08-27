package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1.*

object ExpandWildcardCoalesceMisordered {
  val x1 = a
  val x2 = b
  val x3 = c
}
