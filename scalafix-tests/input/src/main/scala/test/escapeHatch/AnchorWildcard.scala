/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

// when `scalafix:on|off|ok` does not have a rule list it affects every rule
object AnchorWildcard {

  null // assert: NoNull
  val aDummy = 0 // assert: NoDummy

  // scalafix:off
  null
  val bDummy = 0
  // scalafix:on

  null // assert: NoNull
  val cDummy = 0 // assert: NoDummy

  {
    null
    val dummy = 0
  } // scalafix:ok

  null // assert: NoNull
  val eDummy = 0 // assert: NoDummy


  // after a `scalafix:ok`, only rules that were previously enabled should be re-enabled

  // scalafix:off NoNull
  {
    null
    val dummy = 0
  } // scalafix:ok

  null
  val fDummy = 0 // assert: NoDummy
}
