package scalafix.internal.util

import scala.collection.immutable.BitSet

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

  def apply(intervals: List[(Int, Int)]): IntervalSet =
    new IntervalSet(fromRange(intervals))

  private def fromRange(xs: List[(Int, Int)]): BitSet = {
    val n = 64
    val max = xs.maxBy(_._2)._2
    val mask = Array.ofDim[Long](max / n + 1)
    xs.foreach {
      case (start, end) => {
        var i = start
        while (i <= end) {
          val idx = i / n
          mask.update(
            idx,
            mask(idx) | (1L << (i - (idx * n)))
          )
          i += 1
        }
      }
    }
    BitSet.fromBitMaskNoCopy(mask)
  }
}
