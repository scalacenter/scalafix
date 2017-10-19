/*
rule = NoInfer
NoInfer.symbols = [
  "_root_.scala.Predef.any2stringadd(Ljava/lang/Object;)Ljava/lang/Object;."
  "_root_.test.NoInferConfig.B#"
]
*/
package test

case object NoInferConfig {
  case class B()
  List(B()) // assert: NoInfer.b
  def sum[A](a: A, b: String): String = { a + b } // assert: NoInfer.any2stringadd
  new Object() + "abc" // assert: NoInfer.any2stringadd
  val x = List(1, "") // OK, the config clears the defaults
}
