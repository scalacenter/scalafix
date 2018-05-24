package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.Patch
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SyntacticRule

class LegacySyntacticRule(rule: scalafix.Rule)
    extends SyntacticRule(rule.name) {
  private[this] var configuredRule: scalafix.Rule = rule
  override def withConfig(conf: Conf): Configured[Rule] = {
    LegacyRule.init(conf, _ => rule).map { ok =>
      configuredRule = ok
      this
    }
  }
  override def fix(implicit doc: Doc): Patch = {
    val ctx = doc.toRuleCtx
    configuredRule.fix(ctx) + LegacyRule.lints(ctx, configuredRule)
  }
}
