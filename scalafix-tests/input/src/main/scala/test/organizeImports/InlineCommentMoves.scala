/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */

package test.organizeImports

import z.Z // commentZ

import c._ //commentWildcard
import a.A

object InlineCommentMoves {
  val keep = (null: AnyRef)
}

