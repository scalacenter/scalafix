package scalafix.config

import metaconfig.Conf
import metaconfig.ConfDecoder
import java.util.regex.Pattern
import scalafix.internal.config.ScalafixMetaconfigReaders._

class Regex(
    val pattern: Pattern,
    val captureGroup: Option[Int]
)

object Regex {
  implicit val regexDecoder: ConfDecoder[Regex] =
    ConfDecoder.instance[Regex] {
      case obj: Conf.Obj =>
        (obj.get[Pattern]("pattern") |@| obj.getOption[Int]("captureGroup"))
          .map {
            case (pattern, groupIndex) => new Regex(pattern, groupIndex)
          }
    }

  implicit val customMessageRegexDecoder: ConfDecoder[CustomMessage[Regex]] =
    CustomMessage.decoder[Regex](field = "regex")
}
