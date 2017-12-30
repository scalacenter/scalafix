/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
]
*/

// Off and On can be used to create a block that disables a rule

package test

object AnchorIntervals {

  val aDummy = 0 // assert: EscapeHatchDummyLinter

  // scalafix:off EscapeHatchDummyLinter

  val bDummy = 0

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSuppression.Disable

  val cDummy = 0

  // scalafix:on EscapeHatchDummyLinter

  val dDummy = 0 // assert: EscapeHatchDummyLinter

  // scalafix:off EscapeHatchDummyLinter

  val eDummy = 0
}