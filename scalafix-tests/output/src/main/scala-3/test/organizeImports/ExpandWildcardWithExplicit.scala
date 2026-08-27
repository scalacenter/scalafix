package test.organizeImports

import test.organizeImports.MergeImports.Wildcard1.{a, *}

object ExpandWildcardWithExplicit {
  val x1 = a
  val x2 = b
}
