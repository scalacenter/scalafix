/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
  "class:scalafix.test.EscapeHatchDummyLinterB"
]
 */
package test

object EscapeHatchRange {
  //scalafix:off EscapeHatchDummyLinterA
  //scalafix:off EscapeHatchDummyLinterB
  object A
  // scalafix:on EscapeHatchDummyLinterA
  object B
  //scalafix:on EscapeHatchDummyLinterB
}
