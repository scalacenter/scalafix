/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter",
]
*/

package test.escapeHatch

// Unused disable, enable or expressions are reported as a warning

object EscapeHatchUnused {

// Positive Tests (should not report unused)

  // scalafix:off EscapeHatchDummyLinter
  val aDummy = 1
  // scalafix:on EscapeHatchDummyLinter

  val bDummy = 1 // scalafix:ok EscapeHatchDummyLinter

  val cDummy = (
    1,
    2
  ) // scalafix:ok EscapeHatchDummyLinter

  object BDummy { // scalafix:ok EscapeHatchDummyLinter
    val a = 1
  }

// Negative Tests (should report unused)

  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable
  // ...
  /* scalafix:on EscapeHatchDummyLinter */


  /* scalafix:off EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable
  // ...
  /* scalafix:on EscapeHatchDummyLinterTypo */ // assert: UnusedScalafixSupressionEnable

  val ok = 1 /* scalafix:ok EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable

  val okMultiLine = (
    1,
    2
  ) /* scalafix:ok EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable

  object Ok { /* scalafix:ok EscapeHatchDummyLinter */ // assert: UnusedScalafixSupressionDisable
    val a = 1
  }
}

// UnusedScalafixSupression