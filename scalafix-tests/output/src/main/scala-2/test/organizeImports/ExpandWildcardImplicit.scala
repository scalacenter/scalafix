package test.organizeImports

import test.organizeImports.Implicits.a.intImplicit

object ExpandWildcardImplicit {
  def needsInt(implicit i: Int): Int = i
  val x: Int = needsInt
}
