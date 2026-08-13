package test.organizeImports

import test.organizeImports.Implicits.a.intImplicit
import test.organizeImports.MergeImports.Wildcard1.a

object ExpandWildcardImplicitSeparate {
  def needsInt(implicit i: Int): Int = i
  val x: Int = needsInt
  val y = a
}
