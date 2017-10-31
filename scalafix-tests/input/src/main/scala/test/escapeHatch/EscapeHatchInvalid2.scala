/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
]
*/

package test

object EscapeHatchInvalid2 {
  //scalafix:on EscapeHatchDummyLinterA
  object A // assert: EscapeHatchDummyLinterA
  //scalafix:off EscapeHatchDummyLinterA
}