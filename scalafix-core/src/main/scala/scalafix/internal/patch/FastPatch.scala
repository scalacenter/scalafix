package scalafix.internal.patch

import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.v0

object FastPatch {

  def hasSuppression(text: String): Boolean = {
    text.indexOf("scalafix:") >= 0 ||
    text.indexOf("SuppressWarnings") >= 0
  }

  def shortCircuit(
      patchesByName: Map[RuleName, Patch],
      ctx: v0.RuleCtx): Boolean = {
    if (ctx.diffDisable.isEmpty &&
      patchesByName.values.forall(_.isEmpty)) {
      !hasSuppression(ctx.input.text)
    } else {
      false
    }
  }
}
