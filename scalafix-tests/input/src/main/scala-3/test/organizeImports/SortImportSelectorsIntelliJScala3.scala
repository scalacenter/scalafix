/*
rules = [OrganizeImports]
OrganizeImports {
  importSelectorsOrder = IntelliJ
  groupedImports = Keep
  removeUnused = false
  targetDialect = Scala3
}
 */
package test.organizeImports

import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures._
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.A
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.B

// All six selector categories in a single import
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.{b, a, c as C, d as _, given B, given A, *, given}

object SortImportSelectorsIntelliJScala3 {
  object fixtures {
    trait A
    trait B
    object a
    object b
    object c
    object d
    object e
    given A = new A {}
    given B = new B {}
  }
}
