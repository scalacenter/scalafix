/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

package test.escapeHatch

object EscapeHatchPlayground {

  val aDummy = 1 // scalafix:ok EscapeHatchDummyLinter

}