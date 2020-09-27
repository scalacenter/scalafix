package fix

import fix.UnusedImports.a
import fix.UnusedImports.a.v1
import fix.UnusedImports.b
import fix.UnusedImports.c
import fix.UnusedImports.c.{v6 => w2}
import fix.UnusedImports.d
import fix.UnusedImports.d.{v7 => _, _}

object RemoveUnusedRelative {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
