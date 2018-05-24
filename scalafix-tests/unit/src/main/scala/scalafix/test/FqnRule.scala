package banana.rule

import scala.meta._
import scala.meta.contrib._
import scalafix._
import scalafix.v1.Doc
import scalafix.v1.SemanticDoc

case class FqnRule(index: SemanticdbIndex)
    extends SemanticRule(index, "FqnRule") {
  override def fix(ctx: RuleCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable")
}

case object FqnRule2 extends Rule("FqnRule2") {
  override def fix(ctx: RuleCtx): Patch =
    ctx.tree.collectFirst {
      case n: Name => ctx.replaceTree(n, n.value + "2")
    }.asPatch
}

case object PatchTokenWithEmptyRange extends Rule("PatchTokenWithEmptyRange") {
  override def fix(ctx: RuleCtx): Patch = {
    ctx.tokens.collect {
      case tok @ Token.Interpolation.SpliceEnd() =>
        ctx.addRight(tok, "a")
      case tok @ Token.Xml.SpliceEnd() =>
        ctx.addRight(tok, "a")
    }
  }.asPatch
}

class SemanticRuleV1 extends v1.SemanticRule("SemanticRuleV1") {
  override def fix(implicit doc: SemanticDoc): Patch = {
    Patch.addRight(doc.tree, "\nobject SemanticRuleV1")
  }
}

class SyntacticRuleV1 extends v1.SyntacticRule("SyntacticRuleV1") {
  override def fix(implicit doc: Doc): Patch = {
    Patch.addRight(doc.tree, "\nobject SyntacticRuleV1")
  }
}
