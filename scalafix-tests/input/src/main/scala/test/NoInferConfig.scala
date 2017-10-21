/*
rule = NoInfer
NoInfer.symbols = [
  "scala.Predef.any2stringadd"
  "test.NoInferConfig.B."
]
*/
package test

case object NoInferConfig {
  case class B()
  List(B.apply) // assert: NoInfer.b
  List[B](B()) // assert: NoInfer.apply
  def sum[A](a: A, b: String): String = { a + b } // assert: NoInfer.any2stringadd
  new Object() + "abc" // assert: NoInfer.any2stringadd
  val x = List(1, "") // OK, the config clears the defaults
}
