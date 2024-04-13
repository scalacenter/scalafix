/*
rules = [
  "class:scalafix.test.NoDummy",
]
*/
package test.escapeHatch

// On and Off anchor set the filter independently 
// of how many time it was turned off

/* scalafix:off */ // assert: UnusedScalafixSuppression
/* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
/* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
// scalafix:on NoDummy

object AnchorDoubleOn {
  object Dummy // assert: NoDummy
}
