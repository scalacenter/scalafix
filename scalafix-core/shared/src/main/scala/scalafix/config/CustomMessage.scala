package scalafix.config

import metaconfig.{Conf, ConfDecoder}
import scalafix.v0.Symbol
import scalafix.internal.config.ScalafixMetaconfigReaders._

class CustomMessage[T](
    val value: T,
    val message: Option[String],
    val id: Option[String])

object CustomMessage {
  implicit val SymbolDecoder: ConfDecoder[CustomMessage[Symbol.Global]] =
    decoder[Symbol.Global](field = "symbol")
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
                if (msg.contains("\n")) {
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
