package scalafix.internal.v0

import metaconfig.Conf
import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.v0
import scalafix.v1.Configuration
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SyntacticRule

class LegacySyntacticRule(rule: v0.Rule) extends SyntacticRule(rule.name) {
  private[this] var configuredRule: v0.Rule = rule
  override def description: String = rule.description
  override def withConfiguration(
      configuration: Configuration): Configured[Rule] = {
    LegacyRule.init(configuration.conf, _ => rule).map { ok =>
      configuredRule = ok
      this
    }
  }
  override def fix(implicit doc: Doc): Patch = {
    val ctx = new LegacyRuleCtx(doc)
    configuredRule.fix(ctx) + LegacyRule.lints(ctx, configuredRule)
  }
}
