/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
]
*/

package test

object EscapeHatchIntervals {

  val aDummy = 0 // assert: EscapeHatchDummyLinter

  // scalafix:off EscapeHatchDummyLinter

  val bDummy = 0

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable

  val cDummy = 0

  // scalafix:on EscapeHatchDummyLinter

  val dDummy = 0 // assert: EscapeHatchDummyLinter

  // scalafix:off EscapeHatchDummyLinter

  val eDummy = 0
}