package test.organizeImports

import other.P // this used import and comment should stay

object InlineCommentRemovedBlock {
  val keep: P = null.asInstanceOf[P]
}
