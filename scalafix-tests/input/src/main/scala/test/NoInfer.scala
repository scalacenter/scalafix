/*
rule = NoInfer
noInfer.extraSymbols = [
  "_root_.test.NoInfer.B#"
]
*/
package test

case object NoInfer {
  val x = List(1, "")// assert: NoInfer.any
  x.map(x => x -> x)// assert: NoInfer.any
  List[Any](1, "") // OK, not reported message
  List[Any](1, "").map(identity[Any])/*(canBuildFrom[Any])*/// assert: NoInfer.any
  (null: Any)
  null match {
    case _: Any =>
  }
  case class A()
  List(NoInfer, A())// assert: NoInfer.product
  case class B()
  List(B())// assert: NoInfer.b
}
