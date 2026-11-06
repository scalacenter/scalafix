package test.organizeImports

import test.organizeImports.InlineMod2.*
import test.organizeImports.InlineUser.reveal

object InlineMod2 {
  val hidden: Int = 1
}

object InlineUser {
  inline def reveal: Int = InlineMod2.hidden
}

object ExpandWildcardInline {
  val x = reveal
}
