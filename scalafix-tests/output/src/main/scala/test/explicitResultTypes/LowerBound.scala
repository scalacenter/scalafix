package test.explicitResultTypes

object LowerBound {
  trait Bound
  class City extends Bound
  class Town extends Bound
  class Village extends Bound
  sealed abstract class BoundObject[T <: Bound] {
    def list(): List[Bound] = Nil
  }
  case object A extends BoundObject[City]
  case object B extends BoundObject[Town]
  case object C extends BoundObject[Village]
  val x: List[BoundObject[_ <: Bound]] = List(A, B, C)
}