/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */

package test.organizeImports

import z.Z // trailing line comment
import b.B /* trailing block comment */
import a.A

object InlineCommentMixed {
  val keep = 1
}
