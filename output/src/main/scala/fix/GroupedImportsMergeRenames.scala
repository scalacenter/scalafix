package fix

import fix.MergeImports.Rename1.{a => A, b => B, c, d}
import fix.MergeImports.Rename2.{a => A, b => B, c}
import fix.MergeImports.Rename2.{a, b}

object GroupedImportsMergeRenames
