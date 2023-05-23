package test.organizeImports

import test.organizeImports.MergeImports.Rename1.{a => A, b => B, c, d}
import test.organizeImports.MergeImports.Rename2.{a => A, b => B, c}
import test.organizeImports.MergeImports.Rename2.{a, b}

object GroupedImportsMergeRenames
