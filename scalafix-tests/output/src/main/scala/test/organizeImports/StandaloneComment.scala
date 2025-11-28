package test.organizeImports

// This comment is ambiguous and not linked to a specific import
import x.X
import y.Y

object StandaloneComment {
  val keep = new X
  val alsoKeep = new Y
}

