/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  Disable
  NoInfer
]

Disable.symbols = [
  "scala.Option.get"
]

NoInfer.symbols = [
  "scala.Predef.any2stringadd"
]
*/

package test

object EscapeHatchExpression {

  val aDummy = 0 // assert: EscapeHatchDummyLinter

  val bDummy = (
    0,
    1
  ) // scalafix:ok EscapeHatchDummyLinter

  val cDummy = 0 // assert: EscapeHatchDummyLinter

  val foo = (
    0, 
    null // scalafix:ok
  )

  1 + "foo" // scalafix:ok

  1 + "foo" // scalafix:ok NoInfer.any2stringadd

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