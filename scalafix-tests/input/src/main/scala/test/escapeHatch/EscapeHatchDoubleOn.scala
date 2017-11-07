/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

// scalafix:off 
package test.escapeHatch

// scalafix:off EscapeHatchDummyLinter
// scalafix:off EscapeHatchDummyLinter
// scalafix:on EscapeHatchDummyLinter

object EscapeHatchDoubleOn {
  object Dummy // assert: EscapeHatchDummyLinter
}