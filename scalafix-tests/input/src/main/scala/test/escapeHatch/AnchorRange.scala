/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
 */
package test.escapeHatch

// different rule can overlap

object AnchorRange {
  null // assert: NoNull
  val aDummy = 0 // assert: NoDummy
  // scalafix:off NoDummy
  null // assert: NoNull
  // scalafix:off NoNull
  null
  val bDummy = 0
  // scalafix:on NoDummy
  val cDummy = 0 // assert: NoDummy
  null
  // scalafix:on NoNull
  null // assert: NoNull
  val dDummy = 0 // assert: NoDummy
}
