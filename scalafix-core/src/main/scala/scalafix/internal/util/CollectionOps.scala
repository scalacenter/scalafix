package scalafix.internal.util

import scala.collection.immutable.Seq
object CollectionOps {
  def partition[A, B](coll: Seq[A])(
      f: PartialFunction[A, B]): (Seq[A], Seq[B]) = {
    val as = Seq.newBuilder[A]
    val bs = Seq.newBuilder[B]
    val fopt = f.lift
    coll.foreach { a =>
      fopt(a) match {
        case Some(b) => bs += b
        case None => as += a
      }
    }
    as.result() -> bs.result()
  }

}
