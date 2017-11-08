/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

package test.escapeHatch

object EscapeHatchUnused {

  object ok /* scalafix:ok EscapeHatchDummyLinter */ // assert: UnusedScalafixOk

  /* scalafix:on EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression

  val ok2 = 1

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression

}