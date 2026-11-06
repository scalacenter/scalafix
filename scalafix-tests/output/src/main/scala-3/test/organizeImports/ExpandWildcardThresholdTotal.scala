package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1.{a, b, *}

object ExpandWildcardThresholdTotal {
  val x1 = a
  val x2 = b
  val x3 = c
  val x4 = d
}
