package test.organizeImports

import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.*
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.A
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.B

// All six categories in a single import: givens, unimports, renames, wildcard, names, givenAll
import test.organizeImports.SortImportSelectorsIntelliJScala3.fixtures.{a, b, c as C, d as _, *, given A, given B, given}

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
