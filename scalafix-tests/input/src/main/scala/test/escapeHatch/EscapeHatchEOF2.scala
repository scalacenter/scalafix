/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
  "class:scalafix.test.EscapeHatchDummyLinterB"
]
*/

//scalafix:off EscapeHatchDummyLinterA
//scalafix:off EscapeHatchDummyLinterB
package test

object EscapeHatchEOF2 {
  object A
  object B
}
