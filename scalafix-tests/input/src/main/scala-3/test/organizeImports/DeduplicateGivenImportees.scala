/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package test.organizeImports

import test.organizeImports.Givens._
import test.organizeImports.Givens.{given A, given B}
import test.organizeImports.Givens.given A
import test.organizeImports.Givens.given C
import test.organizeImports.Givens.given C

object DeduplicateGivenImportees
