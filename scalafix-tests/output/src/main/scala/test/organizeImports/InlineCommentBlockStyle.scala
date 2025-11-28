package test.organizeImports

import a.A
import b.B
import other.P
import x.X
import y.Y
import z.Z
import z.{ Z1 => Z_1 }
import z.{Z2 => Z_2}
import z.{ Z3 => Z_3 } /* zend */

object InlineCommentBlockStyle {
  val keep = 1
}
