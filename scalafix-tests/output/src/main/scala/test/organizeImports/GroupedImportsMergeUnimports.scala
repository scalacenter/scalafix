package test.organizeImports

import test.organizeImports.MergeImports.Unimport1.{b => B, c => _, d, _}
import test.organizeImports.MergeImports.Unimport2.{a => _, b => _, c => C, d}

object GroupedImportsMergeUnimports
