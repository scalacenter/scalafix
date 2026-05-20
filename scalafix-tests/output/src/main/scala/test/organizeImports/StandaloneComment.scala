package test.organizeImports

import x.X
// This comment is ambiguous and not linked to a specific import
import y.Y

object StandaloneComment {
  val keep = new X
  val alsoKeep = new Y
}

