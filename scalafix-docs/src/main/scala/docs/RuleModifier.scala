package docs
import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input
import scalafix.internal.v1.Rules

class RuleModifier extends StringModifier {
  override val name: String = "rule"
  val rules = Rules.all()
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
