package test.organizeImports

import test.organizeImports.wildcards3.{Apple, topLevelDef, topLevelVal}

object ExpandWildcardTopLevel {
  val x1 = Apple
  val x2 = topLevelDef + topLevelVal
}
