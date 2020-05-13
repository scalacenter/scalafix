/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package fix

import fix.UnusedImports.a.{v1, v2}
import fix.UnusedImports.b.v3
import fix.UnusedImports.c.{v5 => w1, v6 => w2}
import fix.UnusedImports.d.{v7 => unused, _}

object RemoveUnused {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
