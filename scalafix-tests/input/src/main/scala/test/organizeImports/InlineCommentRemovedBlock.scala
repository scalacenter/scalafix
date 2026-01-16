/*
rules = [OrganizeImports]
 */

package test.organizeImports

import my.pkg.K /* this unused import and comment should be removed */
import other.P // this used import and comment should stay

object InlineCommentRemovedBlock {
  val keep: P = null.asInstanceOf[P]
}
