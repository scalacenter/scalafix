/*
rules = [
  "class:scalafix.test.NoDummy",
]
*/

// An anchor disable rules until the end of a file

// scalafix:off NoDummy
package test.escapeHatch

object AnchorEOF {
  object Dummy
}
