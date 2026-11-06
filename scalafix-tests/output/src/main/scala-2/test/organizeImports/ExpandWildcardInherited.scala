package test.organizeImports

import test.organizeImports.ExpandInheritance.Sub.{inherited, own}

object ExpandWildcardInherited {
  val a = own
  val b = inherited
}
