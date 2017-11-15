package scalafix.internal.config

import metaconfig.{Conf, ConfDecoder, Configured}
import org.langmeta._
import MetaconfigPendingUpstream.XtensionConfScalafix

case class DisableSyntaxConfig(
  keywords: Set[DisabledKeyword] = Set()
) {
  implicit val reader: ConfDecoder[DisableSyntaxConfig] =
    ConfDecoder.instanceF[DisableSyntaxConfig](
      _.getField(keywords).map(DisableSyntaxConfig(_)) 
    ) 
}

object DisableSyntaxConfig {
  val default = DisableSyntaxConfig()
  implicit val reader: ConfDecoder[DisableSyntaxConfig] = default.reader
}

case class DisabledKeyword(keyword: String)

object DisabledKeyword {
  private val keywords = Set("foo", "bar")
  private val supportedKeywordsMessage =
    "String, one of: " + 
      keywords.toList.sorted.mkString(" | ")

  implicit val reader: ConfDecoder[DisabledKeyword] =
    new ConfDecoder[DisabledKeyword] {
      override def read(conf: Conf): Configured[DisabledKeyword] = {
        conf match {
          case Conf.Str(keyword) if (keywords.contains(keyword)) =>
            Configured.ok(DisabledKeyword(keyword))
          case _ =>
            Configured.typeMismatch(supportedKeywordsMessage, conf)
        }
      }
    }
}