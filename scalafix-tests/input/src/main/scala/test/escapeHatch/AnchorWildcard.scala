/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

// when `scalafix:on|off|ok` does not have a rule list
// it affects every rules

object AnchorWildcard {
  // null // assert: NoNull
  val aDummy = 0 // assert: NoDummy

  // scalafix:off
  null
  val bDummy = 0
  // scalafix:on

  // null // assert: NoNull
  val cDummy = 0 // assert: NoDummy
}