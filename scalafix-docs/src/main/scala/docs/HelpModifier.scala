package docs

import scala.meta.inputs.Input

import mdoc.Reporter
import mdoc.StringModifier
import scalafix.internal.v1.MainOps

class HelpModifier extends StringModifier {
  val name = "--help"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    "```\n" + MainOps.helpMessage(60) + "\n```"
  }
}
