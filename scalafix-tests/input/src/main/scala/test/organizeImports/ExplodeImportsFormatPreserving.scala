/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Explode
 */

package test.organizeImports

import test.organizeImports.ExplodeImports.FormatPreserving.g1.{ a, b }
import test.organizeImports.ExplodeImports.FormatPreserving.g2.{ c => C, _ }

object ExplodeImportsFormatPreserving
