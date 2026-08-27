package test.organizeImports

import test.organizeImports.Implicits.a.*

object ExpandWildcardImplicit {
  def needsInt(implicit i: Int): Int = i
  val x: Int = needsInt
}
