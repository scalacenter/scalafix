package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta._
import MetaconfigPendingUpstream.XtensionConfScalafix

case class DisableConfig(
  symbols: List[Symbol.Global] = Nil,
  keywords: List[DisableConfig.Keyword] = Nil
) {
  implicit val reader: ConfDecoder[DisableConfig] =
    ConfDecoder.instanceF[DisableConfig] { c =>
      (c.getField(symbols) |@| c.getField(keywords)).map {
        case (a, b) => DisableConfig(a, b)
      }
    }
}

object DisableConfig {
  val empty = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = empty.reader

  sealed trait Keyword
  object Keyword {

    case object Null extends Keyword
    case object Var extends Keyword
    case object While extends Keyword
    case object Throw extends Keyword
    case object Return extends Keyword

    def all = List(Null, Var, While, Throw, Return)

    implicit val readerKeyword: ConfDecoder[Keyword] =
      ReaderUtil.fromMap(all.map(x => x.toString.toLowerCase -> x).toMap)
  }
}
