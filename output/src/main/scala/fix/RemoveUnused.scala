package fix

import fix.UnusedImports.a.v1
import fix.UnusedImports.c.{v6 => w2}
import fix.UnusedImports.d.{v7 => _, _}

object RemoveUnused {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
