package scalafix.config

import scala.collection.immutable.Seq

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured

// TODO(olafur) contribute upstream to metaconfig.
object MetaconfigPendingUpstream {
  def flipSeq[T](lst: Seq[Configured[T]]): Configured[Seq[T]] = {
    lst.foldLeft(Configured.Ok(Seq.empty[T]): Configured[Seq[T]]) {
      case (res, configured) =>
        res.product(configured).map { case (a, b) => b +: a }
    }
  }

  def orElse[T](a: ConfDecoder[T], b: ConfDecoder[T]): ConfDecoder[T] =
    a.orElse(b)
  implicit class XtensionConfDecoderScalafix[T](decoder: ConfDecoder[T]) {
    def orElse(other: ConfDecoder[T]): ConfDecoder[T] =
      new ConfDecoder[T] {
        override def read(conf: Conf): Configured[T] =
          decoder.read(conf) match {
            case ok @ Configured.Ok(_) => ok
            case Configured.NotOk(notOk) =>
              other.read(conf) match {
                case ok2 @ Configured.Ok(_) => ok2
                case Configured.NotOk(notOk2) =>
                  notOk.combine(notOk2).notOk
              }
          }
      }
  }
}
