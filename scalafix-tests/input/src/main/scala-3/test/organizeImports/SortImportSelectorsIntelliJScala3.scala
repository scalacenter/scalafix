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

// All six categories in a single import: givens, unimports, renames, wildcard, names, givenAll
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.{given B, d as _, c as C, *, a, given A, b, given}

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
