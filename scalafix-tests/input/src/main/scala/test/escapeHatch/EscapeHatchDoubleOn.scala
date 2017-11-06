/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
]
*/

//scalafix:off 
package test

// scalafix:off EscapeHatchDummyLinterA
// scalafix:off EscapeHatchDummyLinterA
// scalafix:on EscapeHatchDummyLinterA

object EscapeHatchDoubleOn {
  object A // assert: EscapeHatchDummyLinterA
}