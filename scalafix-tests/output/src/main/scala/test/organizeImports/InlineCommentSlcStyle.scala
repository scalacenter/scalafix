package test.organizeImports

import a.A // A
import b.B
// XYP
// Pbeg
import other.P // Pend
// Xbeg
import x.X // Xend
// Ybeg
import y.Y // Yend
import z.Z // Z
// zbeg
// Z1beg
import z.{Z1 => Z_1} // Z1end // zend
// Z2beg
import z.{Z2 => Z_2} // Z2end
// Z3beg
import z.{Z3 => Z_3} // Z3end

object InlineCommentSlcStyle {
  val keep = 1
}
