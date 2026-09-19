package test.organizeImports

import test.organizeImports.ExpandWildcardInnerWildcard.A.*

object ExpandWildcardInnerWildcard {
  object A {
    object x
    object y
  }
  object B {
    object x
  }
  val outer = x
  val other = y
  object Inner {
    import test.organizeImports.ExpandWildcardInnerWildcard.B._
    val inner = x
  }
}
