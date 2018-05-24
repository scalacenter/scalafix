package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.Patch
import scala.meta.Doc
import scala.meta.Rule
import scala.meta.SyntacticRule

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
    val ctx = doc.toLegacy
    configuredRule.fix(ctx) + LegacyRule.lints(ctx, configuredRule)
  }
}
