package scalafix.internal.config

import scala.collection.immutable.Seq
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.internal.ConfGet

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
    ConfGet.getKey(conf, path +: extraNames) match {
      case Some(value) => ev.read(value)
      case None => ConfError.missingField(conf, path).notOk
    }
  }

  implicit class XtensionConfScalafix(conf: Conf) {
    def getField[T: ConfDecoder](e: sourcecode.Text[T]): Configured[T] =
      conf.getOrElse(e.source)(e.value)
  }
  implicit class XtensionConfiguredScalafix(`_`: Configured.type) {
    def fromEither[T](either: Either[String, T]): Configured[T] =
      either.fold(ConfError.message(_).notOk, Configured.ok)
  }
}
