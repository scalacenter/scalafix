package scalafix.internal.rule

import java.util.regex.Pattern

import pprint.TPrint
import scalafix.config.CustomMessage
import scalafix.config.Regex

class TPrintImplicits {
  implicit val tprintPattern
      : TPrint[List[CustomMessage[Either[Regex, Pattern]]]] =
    TPrint.literal("List[Regex]")
}
