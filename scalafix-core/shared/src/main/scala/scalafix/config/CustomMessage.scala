package scalafix
package config

import metaconfig.{Conf, ConfError, ConfDecoder, Configured, Metaconfig}
import org.langmeta._
import scalafix.internal.config.MetaconfigPendingUpstream.XtensionConfScalafix

import scala.language.implicitConversions

class CustomMessage[T](
    val value: T,
    val message: Option[String],
    val id: Option[String])

object CustomMessage {
  def decoder[T](field: String)(
      implicit ev: ConfDecoder[T]): ConfDecoder[CustomMessage[T]] =
    ConfDecoder.instance[CustomMessage[T]] {
      case obj: Conf.Obj => {
        (obj.get[T](field) |@|
          obj.getOption[String]("message") |@|
          obj.getOption[String]("id")).map {
          case ((value, message0), id) =>
            val message =
              message0.map(msg =>
                if (msg.isMultiline) {
                  "\n" + msg.stripMargin
                } else {
                  msg
              })

            new CustomMessage(value, message, id)
        }
      }
      case els =>
        ev.read(els).map(value => new CustomMessage(value, None, None))
    }
}
