/*
rules = ExplicitResultTypes
*/
package test.explicitResultTypes

object AnyRefWith {
  def ordering = null.asInstanceOf[AnyRef with Ordering[Int]]
  def orderingSeq = null.asInstanceOf[Seq[AnyRef with Ordering[Int]]]
  def orderingObject = null.asInstanceOf[Object with Ordering[Int]]
}