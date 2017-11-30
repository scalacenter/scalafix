package scalafix.tests.util

import org.scalatest.FunSuite
import org.scalactic.source.Position

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
    val set = IntervalSet((1, 2), (5, 8))
    def in(start: Int, end: Int)(implicit pos: Position): Unit  = assert( set.intersect(start, end))
    def out(start: Int, end: Int)(implicit pos: Position): Unit = assert(!set.intersect(start, end))

             // 0 1 2 3 4 5 6 7 8 9 |
             //   <->     <----->   |
             // =================== |
    in(6,7)  //             <->     |
    in(3,5)  //       <--->         |
    in(3,6)  //       <----->       |
    in(5,7)  //           <--->     |
    in(5,6)  //           <->       |
    in(6,7)  //             <->     |
    in(7,8)  //               <->   |
    in(8,9)  //                 <-> |
    in(4,9)  //         <---------> |
             // 0 1 2 3 4 5 6 7 8 9 |
             //   <->     <----->   |
             // =================== |
    out(3,4) //       <->           |
    out(9,9) //                   + |
    out(0,0) // +                   |
  }
  // format:on
}
