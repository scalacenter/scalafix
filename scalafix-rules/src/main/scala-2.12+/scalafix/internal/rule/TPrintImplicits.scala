package scalafix.internal.rule

import java.util.regex.Pattern

import metaconfig.pprint._
import scalafix.config.CustomMessage

class TPrintImplicits {
  implicit val tprintPattern: TPrint[List[CustomMessage[Pattern]]] =
    new TPrint[List[CustomMessage[Pattern]]] {
      def render(implicit tpc: TPrintColors): fansi.Str =
        fansi.Str("List[Regex]")
    }
}
