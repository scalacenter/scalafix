package test.explicitResultTypes

object AnyRefWith {
  def ordering: Ordering[Int] = null.asInstanceOf[AnyRef with Ordering[Int]]
  def orderingSeq: Seq[Ordering[Int]] = null.asInstanceOf[Seq[AnyRef with Ordering[Int]]]
  def orderingObject: Ordering[Int] = null.asInstanceOf[Object with Ordering[Int]]
}