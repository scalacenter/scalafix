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

// Category test 1: names (a, b), renames (c as C), wildcard (*), givenAll (given)
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.{b, c as C, a, *, given}

// Category test 2: names (a, b), renames (c as C), unimports (d => _), givens (given A, given B)
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.{b, c as C, d => _, a, given B, given A}

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
