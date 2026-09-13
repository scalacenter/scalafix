/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

object AnyRefWith {
  def ordering = null.asInstanceOf[AnyRef & Ordering[Int]]
  def orderingSeq = null.asInstanceOf[Seq[AnyRef & Ordering[Int]]]
  def orderingObject = null.asInstanceOf[Object & Ordering[Int]]
}
