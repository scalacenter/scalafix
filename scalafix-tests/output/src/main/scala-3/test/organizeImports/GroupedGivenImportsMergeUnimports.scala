package test.organizeImports

import test.organizeImports.GivenImports.*
import test.organizeImports.GivenImports.{gamma as _, given}
import test.organizeImports.GivenImports2.{
  alpha as _,
  beta as _
}

import test.organizeImports.GivenImports.{
  given Beta,
  given Zeta
}
import test.organizeImports.GivenImports2.{
  given Gamma,
  given Zeta
}

object GroupedGivenImportsMergeUnimports
