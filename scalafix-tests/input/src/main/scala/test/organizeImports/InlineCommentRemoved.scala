/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = true
 */

package test.organizeImports

import my.pkg.K // noteK

import other.P

object InlineCommentRemoved {
  val keep: P = null.asInstanceOf[P]
}

