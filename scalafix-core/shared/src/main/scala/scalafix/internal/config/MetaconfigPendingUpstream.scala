package scalafix.internal.config

import scala.{meta => m}
import metaconfig.Input
import metaconfig.Position
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.internal.ConfGet

// TODO(olafur) contribute upstream to metaconfig.
object MetaconfigPendingUpstream {
  def traverse[T](lst: Seq[Configured[T]]): Configured[List[T]] = {
    val buf = List.newBuilder[T]
    var err = List.newBuilder[ConfError]
    lst.foreach {
      case Configured.Ok(value) =>
        buf += value
      case Configured.NotOk(e) =>
        err += e
    }
    ConfError(err.result()) match {
      case Some(error) => error.notOk
      case _ => Configured.ok(buf.result())
    }
  }
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

  implicit class XtensionMetaconfigInputToMeta(input: Input) {
    def toMeta: m.Input = m.Input.VirtualFile(input.syntax, input.text)
  }
  implicit class XtensionInputToMetaconfig(input: m.Input) {
    def toMetaconfig: Input = Input.VirtualFile(input.syntax, input.text)
  }
  implicit class XtensionPositionToMetaconfig(pos: m.Position) {
    def toMetaconfig: Position = {
      val input = pos.input.toMetaconfig
      val range = Position.Range(input, pos.start, pos.end)
      range
    }
  }

  implicit class XtensionConfScalafix(conf: Conf) {
    def getField[T: ConfDecoder](e: sourcecode.Text[T]): Configured[T] =
      conf.getOrElse(e.source)(e.value)
  }
  implicit class XtensionConfiguredCompanionScalafix(`_`: Configured.type) {
    def fromEither[T](either: Either[String, T]): Configured[T] =
      either.fold(ConfError.message(_).notOk, Configured.ok)
  }
  implicit class XtensionConfiguredScalafix[T](configured: Configured[T]) {
    def foreach(f: T => Unit): Unit = configured match {
      case Configured.Ok(value) => f(value)
      case _ =>
    }
  }
}
