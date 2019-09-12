package test

import scala.collection.breakOut
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LegacySyntheticsTest(xs: List[Int]) {
  def m1(xs: Set[Int]): List[Int] =
    xs.to

  xs.map(x => x)(breakOut): Set[Int]

  Future.sequence(List(Future(1)))(breakOut, global): Future[Seq[Int]]
}
