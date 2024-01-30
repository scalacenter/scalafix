package test.organizeImports

import test.organizeImports.MergeImports.Rename1.{a as A, b as B, c, d}
import test.organizeImports.MergeImports.Rename2.{a as A, b as B, c}
import test.organizeImports.MergeImports.Rename2.{a, b}

object GroupedImportsMergeRenames
