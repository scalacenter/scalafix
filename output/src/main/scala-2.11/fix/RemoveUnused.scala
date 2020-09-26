package fix

import fix.UnusedImports.a.v1
import fix.UnusedImports.a.v2
import fix.UnusedImports.b.v3
import fix.UnusedImports.c.{v5 => w1}
import fix.UnusedImports.c.{v6 => w2}
import fix.UnusedImports.d._
import fix.UnusedImports.d.{v7 => unused}

object RemoveUnused {
  val x1 = v1
  val x2 = w2
  val x3 = v8
}
