package test.organizeImports

import test.organizeImports.wildcards3.{Apple, Box}

object ExpandWildcardInferredNew {
  val x1 = Apple
  val x2 = new Box(1)
}
