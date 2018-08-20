package docs

import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input

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
