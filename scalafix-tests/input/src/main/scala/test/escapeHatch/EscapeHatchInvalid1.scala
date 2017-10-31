/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
]
*/

//scalafix:on EscapeHatchDummyLinterA
package test

object EscapeHatchInvalid1 {
  object A // assert: EscapeHatchDummyLinterA
}