package scalafix.internal.config

import scala.collection.immutable.Seq
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import metaconfig.Metaconfig

// TODO(olafur) contribute upstream to metaconfig.
object MetaconfigPendingUpstream {
  def flipSeq[T](lst: Seq[Configured[T]]): Configured[Seq[T]] = {
    lst.foldLeft(Configured.Ok(Seq.empty[T]): Configured[Seq[T]]) {
      case (res, configured) =>
        res.product(configured).map { case (a, b) => b +: a }
    }
  }
  def getKey[T](conf: Conf.Obj, path: String, extraNames: String*)(
      implicit ev: ConfDecoder[T]): Configured[T] = {
    Metaconfig.getKey(conf, path +: extraNames) match {
      case Some(value) => ev.read(value)
      case None => ConfError.missingField(conf, path).notOk
    }
  }
  def get_![T](configured: Configured[T]): T = configured match {
    case Ok(a) => a
    case NotOk(e) => sys.error(e.toString())
  }

  def orElse[T](a: ConfDecoder[T], b: ConfDecoder[T]): ConfDecoder[T] =
    a.orElse(b)
  implicit class XtensionConfDecoderSeqConfigured[T](lst: Seq[Configured[T]]) {
    def flipSeq: Configured[Seq[T]] =
      MetaconfigPendingUpstream.this.flipSeq(lst)
  }
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
