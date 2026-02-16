/*
rules = [OrganizeImports]
 */

package test.organizeImports

// This comment is ambiguous and not linked to a specific import
import y.Y
import x.X

object StandaloneComment {
  val keep = new X
  val alsoKeep = new Y
}

