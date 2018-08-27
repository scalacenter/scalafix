package scalafix.internal.v0

import metaconfig.Conf
import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.v0
import scalafix.v0.SemanticdbIndex
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule

class LegacySemanticRule(name: RuleName, fn: v0.SemanticdbIndex => v0.Rule)
    extends SemanticRule(name) {
  private[this] var conf: Conf = Conf.Obj()
  override def description: String = fn(SemanticdbIndex.empty).description
  override def withConfiguration(
      configuration: Configuration): Configured[Rule] = {
    LegacyRule.init(configuration.conf, fn).map { _ =>
      this.conf = configuration.conf
      this
    }
  }
  override def fix(implicit sdoc: SemanticDoc): Patch = {
    val ctx = new LegacyRuleCtx(sdoc.internal.doc)
    val rule = fn(new DocSemanticdbIndex(sdoc)).init(this.conf).get
    rule.fix(ctx) + LegacyRule.lints(ctx, rule)
  }
}
