package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.Patch
import scalafix.rule.RuleName
import scalafix.util.SemanticdbIndex
import scala.meta.Rule
import scala.meta.SemanticDoc
import scala.meta.SemanticRule

class LegacySemanticRule(name: RuleName, fn: SemanticdbIndex => scalafix.Rule)
    extends SemanticRule(name) {
  private[this] var conf: Conf = Conf.Obj()
  override def withConfig(conf: Conf): Configured[Rule] = {
    LegacyRule.init(conf, fn).map { _ =>
      this.conf = conf
      this
    }
  }
  override def fix(implicit doc: SemanticDoc): Patch = {
    val ctx = doc.doc.toLegacy
    val rule = fn(doc.toLegacy).init(this.conf).get
    rule.fix(ctx) + LegacyRule.lints(ctx, rule)
  }
}
