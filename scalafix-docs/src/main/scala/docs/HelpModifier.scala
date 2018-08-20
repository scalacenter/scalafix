package docs

import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input
import scalafix.internal.v1.MainOps

class HelpModifier extends StringModifier {
  val name = "--help"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    "```sh\n" + MainOps.helpMessage(60) + "\n```"
  }
}
