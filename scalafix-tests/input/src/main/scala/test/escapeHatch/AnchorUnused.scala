/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

// Unused disable, enable or expressions are reported as a warning

object AnchorUnused {

  // -----------------------------------------------------------------------------
  // Positive Tests (should not report unused)
  // -----------------------------------------------------------------------------

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


  // -----------------------------------------------------------------------------
  // Test case: ON/OFF without any code in between
  // -----------------------------------------------------------------------------

  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
  // ...
  // scalafix:on NoDummy


  // ensure rule is ON
  val dDummy = 0 // assert: NoDummy



  // -----------------------------------------------------------------------------
  // Test case: ON/OFF without any code in between and typo
  // -----------------------------------------------------------------------------

  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
  // ...
  /* scalafix:on NoDummyTypo */ // assert: UnusedScalafixSuppression

  // scalafix:on NoDummy; turn rule back to ON



  // -----------------------------------------------------------------------------
  // Test case: unused OK
  // -----------------------------------------------------------------------------

  val ok = 1 /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression

  val okMultiLine = (
    1,
    2
  ) /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression

  object Ok { /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression
    val a = 1
  }


  // -----------------------------------------------------------------------------
  // Test case: redundant/duplicate ON/OFF
  // -----------------------------------------------------------------------------

  // scalafix:off; turn all rules off before start

  val fDummy = null // ensure rule is OFF

  // scalafix:on NoDummy
  // scalafix:on NoNull
  // scalafix:on
  /* scalafix:on */ // assert: UnusedScalafixSuppression
  /* scalafix:on NoDummy */ // assert: UnusedScalafixSuppression
  /* scalafix:on NoNull */ // assert: UnusedScalafixSuppression


  // ensure rules are ON
  val gDummy = 1 // assert: NoDummy
  null // assert: NoNull


  // scalafix:off NoDummy
  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
  // scalafix:off NoNull
  /* scalafix:off NoNull */ // assert: UnusedScalafixSuppression
  /* scalafix:off */ // assert: UnusedScalafixSuppression
  /* scalafix:off */ // assert: UnusedScalafixSuppression

  val hDummy = null // ensure rules are OFF

  // scalafix:on
  /* scalafix:on */ // assert: UnusedScalafixSuppression
  /* scalafix:on NoDummy */ // assert: UnusedScalafixSuppression
  /* scalafix:on NoNull */ // assert: UnusedScalafixSuppression

  // ensure rules are ON
  val iDummy = 1 // assert: NoDummy
  null // assert: NoNull
}
