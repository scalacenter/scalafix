/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Explode
 */

package fix

import fix.ExplodeImports.FormatPreserving.g1.{ a, b }
import fix.ExplodeImports.FormatPreserving.g2.{ c => C, _ }

object ExplodeImportsFormatPreserving
