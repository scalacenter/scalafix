/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinterA",
]
*/
package test

object EscapeHatchWildcard {
  //scalafix:off
  object A
  //scalafix:on
}