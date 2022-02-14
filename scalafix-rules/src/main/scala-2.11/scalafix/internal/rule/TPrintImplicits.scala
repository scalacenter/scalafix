package scalafix.internal.rule

import java.util.regex.Pattern

import pprint.TPrint
import scalafix.config.CustomMessage

class TPrintImplicits {
  implicit val tprintPattern: TPrint[List[CustomMessage[Pattern]]] =
    TPrint.literal("List[Regex]")
}
