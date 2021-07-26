/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupedImports = Merge
 */
package fix

import fix.GivenImports._
import fix.GivenImports.{alpha => _, given}
import fix.GivenImports.{ given Beta }
import fix.GivenImports.{gamma => _, given}
import fix.GivenImports.{ given Zeta }

import fix.GivenImports2.{alpha => _}
import fix.GivenImports2.{beta => _}
import fix.GivenImports2.{ given Gamma }
import fix.GivenImports2.{ given Zeta }

object GroupedGivenImportsMergeUnimports
