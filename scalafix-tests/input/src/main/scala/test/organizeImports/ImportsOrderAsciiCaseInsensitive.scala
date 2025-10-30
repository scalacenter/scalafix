/*
rules = [OrganizeImports]
OrganizeImports {
  importsOrder = AsciiCaseInsensitive
  removeUnused = false
}
 */
package test.organizeImports

import test.organizeImports.Case.b.c
import test.organizeImports.Case.A.b
import test.organizeImports.Case.A.C
import test.organizeImports.Case.b.b
import test.organizeImports.Case.C.c
import test.organizeImports.Case.A.A
import test.organizeImports.Case.C.a
import test.organizeImports.Case.b.A
import test.organizeImports.Case.C.B

object ImportsOrderAsciiCaseInsensitive
