package test.organizeImports

import test.organizeImports.UnusedImports.a.v1
import test.organizeImports.UnusedImports.c.{v6 => w2}
import test.organizeImports.UnusedImports.d.{v7 => _, _}

object RemoveUnusedMixed {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
