package scalafix.internal.util

import scala.collection.immutable.BitSet

class IntervalSet(range: BitSet) {
  def contains(elem: Int): Boolean =
    range.contains(elem)

  def intersects(start: Int, end: Int): Boolean = {
    val otherRange = BitSet((start to end): _*)
    (range & otherRange).nonEmpty
  }

  override def toString: String = {
    val intervals =
      if (range.isEmpty) Nil
      else {
        var cur = range.head
        var start = cur
        val interval = List.newBuilder[(Int, Int)]
        range.tail.foreach { bit =>
          if (cur + 1 != bit) {
            interval += ((start, cur))
            start = bit
          }
          cur = bit
        }
        interval += ((start, cur))
        interval.result()
      }

    val is = intervals.map {
      case (start, end) =>
        s"[$start, $end]"
    }

    s"""IntervalSet(${is.mkString(", ")})"""

  }
}

object IntervalSet {
  def apply(intervals: Seq[(Int, Int)]): IntervalSet =
    new IntervalSet(fromRange(intervals))

  private def fromRange(xs: Seq[(Int, Int)]): BitSet = {
    if (xs.isEmpty) {
      BitSet()
    } else {
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
}
