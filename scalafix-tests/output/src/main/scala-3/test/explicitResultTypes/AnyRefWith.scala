package test.explicitResultTypes

object AnyRefWith {
  def ordering: Ordering[Int] = null.asInstanceOf[AnyRef & Ordering[Int]]
  def orderingSeq: Seq[Ordering[Int]] = null.asInstanceOf[Seq[AnyRef & Ordering[Int]]]
  def orderingObject: Ordering[Int] = null.asInstanceOf[Object & Ordering[Int]]
}
