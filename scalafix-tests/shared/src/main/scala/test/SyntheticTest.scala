import scala.collection.breakOut
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SyntheticTest {
  def m1(xs: Set[Int]): List[Int] =
    xs.to

  List(1).map(_ + 1)(breakOut): Set[Int]

  Future.sequence(List(Future(1)))(breakOut, global): Future[Seq[Int]]
}
