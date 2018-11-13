package scalafix.config

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured.Ok
import metaconfig.Configured.NotOk
import scalafix.internal.config.ScalafixMetaconfigReaders._
import scalafix.v0.Symbol

class CustomMessage[T](
    val value: T,
    val message: Option[String],
    val id: Option[String])

object CustomMessage {
  implicit val SymbolDecoder: ConfDecoder[CustomMessage[Symbol.Global]] =
    decoder[Symbol.Global](field = "symbol")

  implicit def CustomMessageEitherDecoder[A, B](
      implicit A: ConfDecoder[CustomMessage[A]],
      B: ConfDecoder[CustomMessage[B]]) = {
    def wrapRight(message: CustomMessage[B]): CustomMessage[Either[A, B]] =
      new CustomMessage(Right(message.value), message.message, message.id)

    def wrapLeft(message: CustomMessage[A]): CustomMessage[Either[A, B]] =
      new CustomMessage(Left(message.value), message.message, message.id)

    println("deriving")

    ConfDecoder.instance[CustomMessage[Either[A, B]]] {
      case conf: Conf.Obj =>
        println("B")
        B.read(conf).map(wrapRight) match {
          case ok @ Ok(_) => ok
          case NotOk(err) =>
            println("A")
            A.read(conf).map(wrapLeft) match {
              case ok @ Ok(_) => ok
              case NotOk(err2) =>
                NotOk(
                  ConfError
                    .message(
                      "Failed to decode configuration for either of the following:")
                    .combine(err)
                    .combine(err2)
                )
            }
        }
    }
  }

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
