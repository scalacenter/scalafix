/*
rules = [
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

package test.escapeHatch

object EscapeHatchMultipleRules {

  // scalafix:off NoInfer.any2stringadd, Disable.get
  1 + "foo"


  val a: Option[Int] = Some(1)
  a.get

   /* scalafix:on NoInfer.any2stringadd, Disable.gets */ // assert: UnusedScalafixOn
}
