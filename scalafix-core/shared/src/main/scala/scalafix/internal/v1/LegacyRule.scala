package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.util.SemanticdbIndex

object LegacyRule {
  def init(
      conf: Conf,
      fn: SemanticdbIndex => scalafix.Rule
  ): Configured[scalafix.Rule] = {
    fn(SemanticdbIndex.empty).init(conf) match {
      case Configured.Ok(rule) =>
        Configured.ok(rule)
      case Configured.NotOk(err) => Configured.NotOk(err)
    }
  }
}
