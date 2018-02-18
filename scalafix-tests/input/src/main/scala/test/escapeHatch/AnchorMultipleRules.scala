/*
rules = [
  Disable
  NoInfer
  "class:scalafix.test.EscapeHatchDummyLinter"
]

Disable.symbols = [
  "scala.Option.get"
]

NoInfer.symbols = [
  "scala.Predef.any2stringadd"
]
*/

// rules can be selectively disabled by providing a list of rule
// ids separated by commas

package test.escapeHatch

object AnchorMultipleRules {

  // scalafix:off NoInfer.any2stringadd, Disable.get
  Some(1) + "foo"

  val aDummy = 0 // assert: EscapeHatchDummyLinter

  val a: Option[Int] = Some(1)
  a.get

  /* scalafix:on NoInfer.any2stringadd, Disable.get */
}
