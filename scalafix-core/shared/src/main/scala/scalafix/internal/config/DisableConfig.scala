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

  val keywordsSet: Set[String] = keywords.map(_.show).toSet
}

object DisableConfig {
  val empty = DisableConfig()
  implicit val reader: ConfDecoder[DisableConfig] = empty.reader

  sealed trait Keyword { def show: String }
  object Keyword {
    case object Null extends Keyword { def show: String = "null" }
    case object Var extends Keyword { def show: String = "var" }
    case object While extends Keyword { def show: String = "while" }
    case object Throw extends Keyword { def show: String = "throw" }
    case object Return extends Keyword { def show: String = "return" }

    def all = List(Null, Var, While, Throw, Return)

    implicit val readerKeyword: ConfDecoder[Keyword] =
      ReaderUtil.fromMap(all.map(x => x.show -> x).toMap)
  }
}
