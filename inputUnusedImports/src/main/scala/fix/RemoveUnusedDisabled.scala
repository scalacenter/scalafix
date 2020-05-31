/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = false
}
 */
package fix

import fix.UnusedImports.a.{v1, v2}
import fix.UnusedImports.b.v3
import fix.UnusedImports.c.{v5 => w1, v6 => w2}
import fix.UnusedImports.d.{v7 => unused, _}

object RemoveUnusedDisabled {
  import fix.UnusedImports.e.v9

  val x1 = v1
  val x2 = w2
  val x3 = v8
}
