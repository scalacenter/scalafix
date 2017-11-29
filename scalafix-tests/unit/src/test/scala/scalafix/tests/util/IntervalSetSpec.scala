package scalafix.tests.util

import org.scalatest.FunSuite

import scalafix.internal.util.IntervalSet

class IntervalSetSpec() extends FunSuite {
  // format: off
  test("contains") {    
    val set = IntervalSet((1, 2), (4, 5))
    assert(!set.contains(0))
    assert( set.contains(1))
    assert( set.contains(2))
    assert(!set.contains(3))
    assert( set.contains(4))
    assert( set.contains(5))
    assert(!set.contains(6))
  }

  test("intersect") {
                                          // -1 0 1 2 3 4 5 6 7 8 9 |
    val set = IntervalSet((0, 2), (5, 7)) //    <--->     <--->     |
                                          // ==================     |
    assert(set.intersect(-1, 3))          // <-------->             | ∩
    assert(set.intersect(1, 5))           //      <------->         | ∩
    assert(!set.intersect(3, 4))          //          <->           | x
    assert(set.intersect(2, 4))           //        <--->           | ∩
    assert(!set.intersect(8, 9))          //                    <-> | x
                                          //                        |
    val set2 = IntervalSet((0, 9))        //    <-----------------> |
    assert(set.intersect(3, 5))           //          <--->         | ∩
                                          // -1 0 1 2 3 4 5 6 7 8 9 |
  }
  // format:on
}
