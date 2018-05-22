package scalafix.internal.v1

import scalafix.Patch
import scalafix.v1.Doc
import scalafix.v1.SyntacticRule

class LegacySyntacticRule(rule: scalafix.Rule)
    extends SyntacticRule(rule.name) {
  override def fix(implicit doc: Doc): Patch = {
    val ctx = doc.toLegacy
    rule.fix(ctx)
  }
}
