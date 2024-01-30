/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
OrganizeImports.targetDialect = Scala3
 */

package test.organizeImports

import test.organizeImports.GivenImports.*
import test.organizeImports.GivenImports.{ given Alpha, given Beta }

object MergeImportsFormatPreservingGiven
