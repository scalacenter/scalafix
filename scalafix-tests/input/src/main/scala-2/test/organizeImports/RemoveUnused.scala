/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package test.organizeImports

import test.organizeImports.UnusedImports.a.{v1, v2}
import test.organizeImports.UnusedImports.b.v3
import test.organizeImports.UnusedImports.c.{v5 => w1, v6 => w2}
import test.organizeImports.UnusedImports.d.{v7 => unused, _}

object RemoveUnused {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
