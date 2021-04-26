/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

object AnchorOverlapping {

  // ensure rules are ON
  val dummy0 = 0 // assert: NoDummy
  null // assert: NoNull


  // -----------------------------------------------------------------------------
  // Test case: when there is a complete overlapping between 'off|on' and 'ok',
  // the outermost one should prevail and the other should be reported as unused
  // -----------------------------------------------------------------------------

  {
    /* scalafix:off */ // assert: UnusedScalafixSuppression
    val dummy = 0
    null
    // scalafix:on
  } // scalafix:ok


  // ensure rules are ON
  val dummy1 = 0 // assert: NoDummy
  null // assert: NoNull


  // scalafix:off
  {
    val dummy = 0
    null
  } /* scalafix:ok */ // assert: UnusedScalafixSuppression
  // scalafix:on


  // ensure rules are ON
  val dummy2 = 0 // assert: NoDummy
  null // assert: NoNull



  // -----------------------------------------------------------------------------
  // Test case: when there is a partial overlapping between 'off|on' and 'ok', the
  // one that starts first should prevail and the other should be reported as unused
  // -----------------------------------------------------------------------------

  // scalafix:off
  {
    val dummy = 0
    null
    // scalafix:on
  } /* scalafix:ok */ // assert: UnusedScalafixSuppression


  // ensure rules are ON
  val dummy3 = 0 // assert: NoDummy
  null // assert: NoNull


  {
    /* scalafix:off */ // assert: UnusedScalafixSuppression
    val dummy = 0
    null
  } // scalafix:ok
  // scalafix:on


  // ensure rules are ON
  val dummy4 = 0 // assert: NoDummy
  null // assert: NoNull


  // -----------------------------------------------------------------------------
  // Test case: partial overlapping between 'off|on' and 'ok' should not report
  // unused if both escapes are triggered
  // -----------------------------------------------------------------------------

  // scalafix:off
  {
    val dummy = 0
    // scalafix:on
    null
  } // scalafix:ok


  // ensure rules are ON
  val dummy5 = 0 // assert: NoDummy
  null // assert: NoNull


  {
    // scalafix:off
    val dummy = 0
    null
  } // scalafix:ok
  null
  // scalafix:on


  // ensure rules are ON
  val dummy6 = 0 // assert: NoDummy
  null // assert: NoNull



  // -----------------------------------------------------------------------------
  // Test case: a 'on' should not re-enable a rule within the range of a 'ok' and
  // it should be reported as unused.
  // -----------------------------------------------------------------------------

  {
    /* scalafix:on */ // assert: UnusedScalafixSuppression
    val dummy = 0
    null
  } // scalafix:ok


  // ensure rules are ON
  val dummy7 = 0 // assert: NoDummy
  null // assert: NoNull



  // -----------------------------------------------------------------------------
  // Test case: overlapping 'off|on' and 'ok' should not conflict if they target
  // different rules
  // -----------------------------------------------------------------------------

  // scalafix:off NoNull
  {
    val dummy = 0
    null
  } // scalafix:ok NoDummy
  // scalafix:on NoNull


  // ensure rules are ON
  val dummy8 = 0 // assert: NoDummy
  null // assert: NoNull


  {
    val dummy = 0
    // scalafix:off NoNull
    null
    // scalafix:on NoNull
  } // scalafix:ok NoDummy


  // ensure rules are ON
  val dummy9 = 0 // assert: NoDummy
  null // assert: NoNull


  // scalafix:off NoNull
  {
    val dummy = 0
    null
    // scalafix:on NoNull
  } // scalafix:ok NoDummy


  // ensure rules are ON
  val dummy10 = 0 // assert: NoDummy
  null // assert: NoNull


  {
    val dummy = 0
    // scalafix:off NoNull
    null
  } // scalafix:ok NoDummy
  // scalafix:on NoNull


  // ensure rules are ON
  val dummy11 = 0 // assert: NoDummy
  null // assert: NoNull
}
