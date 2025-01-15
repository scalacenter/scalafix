package test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LegacySyntheticsTest(xs: List[Int]) {
  def m1(xs: Set[Int]): List[Int] =
    xs.to(scala.collection.immutable.List)

  xs.iterator.map(x => x).to(scala.collection.immutable.Set): Set[Int]

  Future.sequence(List(Future(1)))(scala.collection.immutable.List, global): Future[Seq[Int]]
}
