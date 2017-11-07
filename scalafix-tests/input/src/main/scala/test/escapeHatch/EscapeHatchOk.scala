/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]
*/

package test

object EscapeHatchOk {

  val aDummy = (
    0,
    1
  ) // scalafix:ok EscapeHatchDummyLinter

  val foo = (
    0, 
    null // scalafix:ok EscapeHatchNoNulls
  ) 
}