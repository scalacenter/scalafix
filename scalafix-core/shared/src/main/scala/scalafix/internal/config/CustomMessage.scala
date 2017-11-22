package scalafix
package internal.config

import metaconfig.{Conf, ConfError, ConfDecoder, Configured}
import org.langmeta._
import MetaconfigPendingUpstream.XtensionConfScalafix

import scala.language.implicitConversions

case class CustomMessage[T](value: T, message: Option[String])

object CustomMessage {
  def decoder[T](field: String)(
      implicit ev: ConfDecoder[T]): ConfDecoder[CustomMessage[T]] =
    ConfDecoder.instance[CustomMessage[T]] {
      case obj: Conf.Obj =>
        (obj.get[T](field) |@| obj.get[String]("message")).map {
          case (value, message0) => {
            val message =
              if (message0.isMultiline) {
                "\n" + message0.stripMargin
              } else {
                message0
              }
            CustomMessage(value, Some(message))
          }
        }
      case els =>
        ev.read(els).map(value => CustomMessage(value, None))
    }
}
