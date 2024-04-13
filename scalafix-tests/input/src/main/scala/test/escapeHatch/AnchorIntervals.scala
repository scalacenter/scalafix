/*
rules = [
  "class:scalafix.test.NoDummy"
]
*/

// Off and On can be used to create a block that disables a rule

package test

object AnchorIntervals {

  val aDummy = 0 // assert: NoDummy

  // scalafix:off NoDummy

  val bDummy = 0

  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression

  val cDummy = 0

  // scalafix:on NoDummy

  val dDummy = 0 // assert: NoDummy

  // scalafix:off NoDummy

  val eDummy = 0
}
