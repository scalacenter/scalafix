/*
rules = [
  "class:scalafix.test.NoDummy",
]
*/

package test.escapeHatch

// Unused disable, enable or expressions are reported as a warning

object AnchorUnused {

// Positive Tests (should not report unused)

  // scalafix:off NoDummy
  val aDummy = 1
  // scalafix:on NoDummy

  val bDummy = 1 // scalafix:ok NoDummy

  val cDummy = (
    1,
    2
  ) // scalafix:ok NoDummy

  object BDummy { // scalafix:ok NoDummy
    val a = 1
  }

// Negative Tests (should report unused)

  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression.Disable
  // ...
  /* scalafix:on NoDummy */


  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression.Disable
  // ...
  /* scalafix:on NoDummyTypo */ // assert: UnusedScalafixSuppression.Enable

  val ok = 1 /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression.Disable

  val okMultiLine = (
    1,
    2
  ) /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression.Disable

  object Ok { /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression.Disable
    val a = 1
  }
}