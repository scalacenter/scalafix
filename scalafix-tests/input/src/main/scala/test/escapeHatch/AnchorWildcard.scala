/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]
*/
package test.escapeHatch

// when `scalafix:on|off|ok` does not have a rule list
// it affects every rules

object AnchorWildcard {
  // null // assert: EscapeHatchNoNulls
  val aDummy = 0 // assert: EscapeHatchDummyLinter

  // scalafix:off
  null
  val bDummy = 0
  // scalafix:on

  // null // assert: EscapeHatchNoNulls
  val cDummy = 0 // assert: EscapeHatchDummyLinter
}