package scalafix.internal.util

import scala.collection.BitSet

class IntervalSet(range: BitSet) {
  def contains(elem: Int): Boolean =
    range.contains(elem)

  def intersects(start: Int, end: Int): Boolean = {
    val otherRange = BitSet((start to end): _*)
    (range & otherRange).nonEmpty
  }
}

object IntervalSet {
  def apply(intervals: (Int, Int)*): IntervalSet =
    apply(intervals.toList)

  def apply(intervals: List[(Int, Int)]): IntervalSet = {
    val range = BitSet.newBuilder
    intervals.foreach {
      case (start, end) =>
        range ++= BitSet((start to end): _*)
    }
    new IntervalSet(range.result)
  }
}
