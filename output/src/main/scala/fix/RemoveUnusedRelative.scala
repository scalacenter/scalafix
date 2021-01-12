package fix

import fix.UnusedImports.a.v1
import fix.UnusedImports.c.{v6 => w2}
import fix.UnusedImports.d.{v7 => _, _}
import fix.UnusedImports.{a, b, c, d}

object RemoveUnusedRelative {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
