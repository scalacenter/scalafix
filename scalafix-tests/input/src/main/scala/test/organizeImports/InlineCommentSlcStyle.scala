/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */

package test.organizeImports

import z.Z // Z

import a.A // A

import b.B

// XYP
import
  // Xbeg
  x.X, // Xend
  // Ybeg
  y.Y, // Yend
  // Pbeg
  other.P // Pend

// zbeg
import z.{
  // Z1beg
  Z1 => Z_1, // Z1end
  // Z2beg
  Z2 => Z_2, // Z2end
  // Z3beg
  Z3 => Z_3 // Z3end
} // zend

object InlineCommentSlcStyle {
  val keep = 1
}
