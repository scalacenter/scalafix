/*
rule = NoInfer
*/
package test

case object NoInfer {
  List(1, "")// scalafix: NoInfer.any
  List[Any](1, "")
  (null: Any)
  null match {
    case _: Any =>
  }
  case class A()
  List(NoInfer, A())// scalafix: NoInfer.product
}
