/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */

package test.organizeImports

// Leading comment for Z
import z.Z

// Leading comment for A
import a.A

import b.B

object InlineCommentLeading {
  val keep = 1
}
