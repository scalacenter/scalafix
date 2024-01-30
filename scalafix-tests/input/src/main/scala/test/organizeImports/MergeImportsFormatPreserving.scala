/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
OrganizeImports.targetDialect = Auto
 */

package test.organizeImports

import test.organizeImports.MergeImports.FormatPreserving.g1.{ a, b => B }
import test.organizeImports.MergeImports.FormatPreserving.g2._
import test.organizeImports.MergeImports.FormatPreserving.g2.{ d => D }

object MergeImportsFormatPreserving
