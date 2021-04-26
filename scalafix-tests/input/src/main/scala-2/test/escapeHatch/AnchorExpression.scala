/*
rules = [
  "class:scalafix.test.NoDummy"
  Disable
  Disable
]

Disable.symbols = [
  "scala.None"
  "scala.Option.get"
]
*/

// `scalafix:ok` can be used to disable expressions
// it can be attached to an expressions that spans multiple lines
// or inside the first line of the template of an object/class/trait

package test.escapeHatch

object AnchorExpression {

  val aDummy = 0 // assert: NoDummy

  val bDummy = (
    0,
    1
  ) // scalafix:ok NoDummy

  val cDummy = 0 // assert: NoDummy

  None + "foo" // scalafix:ok Disable.None

  val a: Option[Int] = Some(1)

  (
    null,
    None + "foo",
    a.get
  ) // scalafix:ok Disable.None, Disable.get

  object A {
    object F {
      object Dummy { // scalafix:ok NoDummy
        1
      }
    }
  }
}
