/*
rules = [OrganizeImports]
OrganizeImports {
  expandWildcardImportThreshold = 5
  groupedImports = Keep
  removeUnused = false
  targetDialect = Scala3
}
 */
package test.organizeImports

import test.organizeImports.ExtSyntax.*

object ExtSyntax {
  extension (s: String) def shout: String = s
  val plain: Int = 1
}

object ExpandWildcardExtension {
  val a = "hi".shout
  val b = plain
}
