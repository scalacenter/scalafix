package scalafix.internal.rule

import scalafix._
import scalafix.internal.config._
import scalafix.rule.Rule
import metaconfig.ConfError
import metaconfig.Configured

object ConfigRule {
  def apply(
      patches: ConfigRulePatches,
      getSemanticdbIndex: LazySemanticdbIndex): Configured[Option[Rule]] = {
    val configurationPatches = patches.all
    if (configurationPatches.isEmpty) Configured.Ok(None)
    else {
      getSemanticdbIndex(RuleKind.Semantic) match {
        case None =>
          ConfError
            .message(".scalafix.conf patches require the Semantic API.")
            .notOk
        case Some(index) =>
          val rule = Rule.constant(
            ".scalafix.conf",
            configurationPatches.asPatch,
            index
          )
          Configured.Ok(Some(rule))
      }
    }
  }

}
