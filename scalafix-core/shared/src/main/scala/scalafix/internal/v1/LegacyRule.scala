package scalafix.internal.v1

import scalafix.Patch
import scalafix.rule.RuleName
import scalafix.util.SemanticdbIndex
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule

class LegacyRule(name: RuleName, fn: SemanticdbIndex => scalafix.Rule)
  extends SemanticRule(name) {
  override def fix(implicit doc: SemanticDoc): Patch = {
    val ctx = doc.doc.toLegacy
    val rule = fn(doc.toLegacy)
    rule.fix(ctx)
  }
}
