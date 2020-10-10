package docs
import scala.meta.inputs.Input

import mdoc.Reporter
import mdoc.StringModifier
import scalafix.internal.v1.Rules
import scalafix.v1.Rule

class RuleModifier extends StringModifier {
  override val name: String = "rule"
  val rules: List[Rule] = Rules.all()
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    rules.find(_.name.matches(info)) match {
      case Some(rule) =>
        rule.description
      case None =>
        reporter.error(s"No rule named '$info'")
        ""
    }
  }
}
