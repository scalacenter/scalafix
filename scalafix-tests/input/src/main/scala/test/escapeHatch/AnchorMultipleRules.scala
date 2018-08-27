/*
rules = [
  Disable
  Disable
  "class:scalafix.test.NoDummy"
]

Disable.symbols = [
  "scala.None"
  "scala.Option.get"
]

*/

// rules can be selectively disabled by providing a list of rule
// ids separated by commas

package test.escapeHatch

object AnchorMultipleRules {

  // scalafix:off Disable.None, Disable.get
  None + "foo"

  val aDummy = 0 // assert: NoDummy

  val a: Option[Int] = Some(1)
  a.get

  /* scalafix:on Disable.None, Disable.get */
}
