package docs

import scala.meta.inputs.Input

import mdoc.Reporter
import mdoc.StringModifier

class RulesModifier extends StringModifier {
  val name = "scalafix-rules"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    scalafix.website.allRulesTable(reporter)
  }
}
