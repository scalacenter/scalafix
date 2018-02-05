/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  Disable
  NoInfer
]

Disable.parts = [
  {
    symbols = ["scala.Option.get"]
  }
]

NoInfer.symbols = [
  "scala.Predef.any2stringadd"
]
*/

// `scalafix:ok` can be used to disable expressions
// it can be attached to an expressions that spans multiple lines
// or inside the first line of the template of an object/class/trait

package test.escapeHatch

object EscapeHatchExpression {

  val aDummy = 0 // assert: EscapeHatchDummyLinter

  val bDummy = (
    0,
    1
  ) // scalafix:ok EscapeHatchDummyLinter

  val cDummy = 0 // assert: EscapeHatchDummyLinter

  Some(1) + "foo" // scalafix:ok NoInfer.any2stringadd

  val a: Option[Int] = Some(1)

  (
    null,
    1 + "foo",
    a.get
  ) // scalafix:ok NoInfer.any2stringadd, Disable.get

  object A {
    object F {
      object Dummy { // scalafix:ok EscapeHatchDummyLinter
        1
      }
    }
  }
}