/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/
package test.escapeHatch

// On and Off anchor set the filter independently 
// of how many time it was turned off

/* scalafix:off */ // assert: UnusedScalafixSupression.Disable
/* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression.Disable
/* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression.Disable
// scalafix:on EscapeHatchDummyLinter

object EscapeHatchDoubleOn {
  object Dummy // assert: EscapeHatchDummyLinter
}