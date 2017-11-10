/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]
 */
package test.escapeHatch

// different rule can overlap

object EscapeHatchRange {
  null // assert: EscapeHatchNoNulls
  val aDummy = 0 // assert: EscapeHatchDummyLinter
  // scalafix:off EscapeHatchDummyLinter
  null // assert: EscapeHatchNoNulls
  // scalafix:off EscapeHatchNoNulls
  null
  val bDummy = 0
  // scalafix:on EscapeHatchDummyLinter
  val cDummy = 0 // assert: EscapeHatchDummyLinter
  null
  // scalafix:on EscapeHatchNoNulls
  null // assert: EscapeHatchNoNulls
  val dDummy = 0 // assert: EscapeHatchDummyLinter
}
