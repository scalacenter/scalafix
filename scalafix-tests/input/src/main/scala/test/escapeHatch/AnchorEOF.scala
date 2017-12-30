/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

// An anchor disable rules until the end of a file

// scalafix:off EscapeHatchDummyLinter
package test.escapeHatch

object AnchorEOF {
  object Dummy
}