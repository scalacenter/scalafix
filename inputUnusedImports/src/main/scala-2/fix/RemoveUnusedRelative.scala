/*
rules = [OrganizeImports]
OrganizeImports {
  expandRelative = true
  groupedImports = Explode
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package fix

import fix.UnusedImports.a
import fix.UnusedImports.b
import fix.UnusedImports.c
import fix.UnusedImports.d

import a.{v1, v2}
import b.v3
import c.{v5 => w1, v6 => w2}
import d.{v7 => unused, _}

object RemoveUnusedRelative {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
