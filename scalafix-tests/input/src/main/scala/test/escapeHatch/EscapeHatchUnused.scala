/* ONLY
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

package test.escapeHatch

object EscapeHatchUnused {

  // positive tests

  // scalafix:off EscapeHatchDummyLinter
  val bDummy = 1
  // scalafix:on EscapeHatchDummyLinter


  // negative tests

  // Matching but not triggered

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression

  /* scalafix:on EscapeHatchDummyLinter */

  // Not matching because of a typo

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression

  /* scalafix:on EscapeHatchDummyLinterTypo */ // assert: UnusedScalafixSupression
}