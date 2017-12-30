/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/
package test.escapeHatch

// On and Off anchor set the filter independently 
// of how many time it was turned off

/* scalafix:off */ // assert: UnusedScalafixSuppression.Disable
/* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSuppression.Disable
/* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSuppression.Disable
// scalafix:on EscapeHatchDummyLinter

object AnchorDoubleOn {
  object Dummy // assert: EscapeHatchDummyLinter
}