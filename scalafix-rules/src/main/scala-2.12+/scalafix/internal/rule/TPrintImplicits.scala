package scalafix.internal.rule

import java.util.regex.Pattern

import metaconfig.pprint._
import scalafix.config.CustomMessage
import scalafix.config.Regex

class TPrintImplicits {
  implicit val tprintPattern
      : TPrint[List[CustomMessage[Either[Regex, Pattern]]]] =
    new TPrint[List[CustomMessage[Either[Regex, Pattern]]]] {
      def render(implicit tpc: TPrintColors): fansi.Str =
        fansi.Str("List[Regex]")
    }
}
