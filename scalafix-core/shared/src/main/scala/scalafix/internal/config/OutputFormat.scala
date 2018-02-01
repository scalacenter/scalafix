package scalafix.internal.config

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import MetaconfigPendingUpstream._

sealed abstract class OutputFormat
object OutputFormat {
  def apply(arg: String): Either[String, OutputFormat] = {
    arg match {
      case OutputFormat(format) =>
        Right(format)
      case els =>
        Left(s"Expected one of ${all.mkString(", ")}, obtained $els")
    }
  }
  def all: Seq[OutputFormat] = List(Default, Sbt)
  def unapply(arg: String): Option[OutputFormat] =
    all.find(_.toString.equalsIgnoreCase(arg))
  case object Default extends OutputFormat
  case object Sbt extends OutputFormat
  implicit val decoder: ConfDecoder[OutputFormat] =
    ConfDecoder.instance[OutputFormat] {
      case Conf.Str(str) => Configured.fromEither(OutputFormat(str))
    }
}
