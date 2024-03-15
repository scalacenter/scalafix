/*
rules = [
  "class:scalafix.test.NoDummy"
]
*/
package test.escapeHatch

object AnchorDescription {

  // ensure rule is ON
  val dummy0 = 0 // assert: NoDummy


  // -----------------------------------------------------------------------------
  // Test case: off|on + wildcard + description
  // -----------------------------------------------------------------------------

  // scalafix:off; turning all rules OFF
  val dummy1 = 0

  // scalafix:on; turning all rules ON
  val dummy2 = 0 // assert: NoDummy


  // -----------------------------------------------------------------------------
  // Test case: off|on + specific rules + description
  // -----------------------------------------------------------------------------

  // scalafix:off NoDummy; turning rule OFF
  val dummy3 = 0

  // scalafix:on NoDummy; turning rule ON
  val dummy4 = 0 // assert: NoDummy


  // -----------------------------------------------------------------------------
  // Test case: off|on + missing description after ';'
  // -----------------------------------------------------------------------------

  // scalafix:off;
  val dummy5 = 0

  // scalafix:on;
  val dummy6 = 0 // assert: NoDummy

  // scalafix:off ;
  val dummy7 = 0

  // scalafix:on ;
  val dummy8 = 0 // assert: NoDummy


  // -----------------------------------------------------------------------------
  // Test case: ok + wildcard + description
  // -----------------------------------------------------------------------------

  val dummy9 = 0 // scalafix:ok; disabling all rules


  // -----------------------------------------------------------------------------
  // Test case: ok + specific rules + description
  // -----------------------------------------------------------------------------

  val dummy10 = 0 // scalafix:ok NoDummy; disabling single rule


  // -----------------------------------------------------------------------------
  // Test case: ok + missing description after ';'
  // -----------------------------------------------------------------------------

  val dummy11 = 0 // scalafix:ok;
  val dummy12 = 0 // scalafix:ok ;


  // -----------------------------------------------------------------------------
  // Test case: description containing multiple ';' characters
  // -----------------------------------------------------------------------------

  val dummy13 = 0 // scalafix:ok; apple; orange; banana;
  val dummy14 = 0 // scalafix:ok NoDummy; apple; orange; banana;
}
