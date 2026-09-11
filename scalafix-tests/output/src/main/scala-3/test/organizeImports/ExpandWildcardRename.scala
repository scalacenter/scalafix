package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1.{a as a1, *}

object ExpandWildcardRename {
  val x1 = a1
  val x2 = b
}
