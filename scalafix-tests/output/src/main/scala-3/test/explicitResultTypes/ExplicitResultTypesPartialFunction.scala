package test.explicitResultTypes

object PartialFunction {
  def empty[A, B]: scala.PartialFunction[A, B] = scala.PartialFunction.empty[A, B]
}
