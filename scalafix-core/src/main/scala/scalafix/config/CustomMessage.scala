package scalafix.config

import metaconfig.Conf
import metaconfig.ConfDecoder
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.v0.Symbol

class CustomMessage[T](
    val value: T,
    val message: Option[String],
    val id: Option[String]
)

object CustomMessage {
  implicit val SymbolDecoder: ConfDecoder[CustomMessage[Symbol.Global]] =
    decoder[Symbol.Global](field = "symbol")(ScalafixMetaconfigReaders.symbolGlobalReader)

  implicit def CustomMessageEitherDecoder[A, B](implicit
      AB: ConfDecoder[Either[CustomMessage[A], CustomMessage[B]]]
  ): ConfDecoder[CustomMessage[Either[A, B]]] =
    AB.map {
      case Right(msg) =>
        new CustomMessage(Right(msg.value), msg.message, msg.id)
      case Left(msg) => new CustomMessage(Left(msg.value), msg.message, msg.id)
    }

  def decoder[T](
      field: String
  )(implicit ev: ConfDecoder[T]): ConfDecoder[CustomMessage[T]] =
    ConfDecoder.instance[CustomMessage[T]] {
      case obj: Conf.Obj => {
        (obj.get[T](field) |@|
          obj.getOption[String]("message") |@|
          obj.getOption[String]("id")).map { case ((value, message0), id) =>
          val message =
            message0.map(msg =>
              if (msg.contains("\n")) {
                "\n" + msg.stripMargin
              } else {
                msg
              }
            )

          new CustomMessage(value, message, id)
        }
      }
      case els =>
        ev.read(els).map(value => new CustomMessage(value, None, None))
    }
}
