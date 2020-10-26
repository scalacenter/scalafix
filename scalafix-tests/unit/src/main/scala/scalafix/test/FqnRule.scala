package banana.rule

import scala.meta._
import scala.meta.contrib._

import scalafix.patch.Patch
import scalafix.util.SymbolMatcher
import scalafix.v0
import scalafix.v1

case class FqnRule(index: v0.SemanticdbIndex)
    extends v0.SemanticRule(index, "FqnRule") {
  override def fix(ctx: v0.RuleCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable") + {
      val fqnRule = SymbolMatcher.exact(v0.Symbol("test/FqnRule."))
      ctx.tree.collect {
        case fqnRule(t: Term.Name) =>
          ctx.addLeft(t, "/* matched */ ")
      }.asPatch
    }
}

case object FqnRule2 extends v0.Rule("FqnRule2") {
  override def fix(ctx: v0.RuleCtx): Patch =
    ctx.tree.collectFirst {
      case n: Name => ctx.replaceTree(n, n.value + "2")
    }.asPatch
}

case object PatchTokenWithEmptyRange
    extends v0.Rule("PatchTokenWithEmptyRange") {
  override def fix(ctx: v0.RuleCtx): Patch = {
    ctx.tokens.collect {
      case tok @ Token.Interpolation.SpliceEnd() =>
        ctx.addRight(tok, "a")
      case tok @ Token.Xml.SpliceEnd() =>
        ctx.addRight(tok, "a")
    }
  }.asPatch
}

class SemanticRuleV1 extends v1.SemanticRule("SemanticRuleV1") {
  override def fix(implicit doc: v1.SemanticDocument): Patch = {
    Patch.addRight(doc.tree, "\nobject SemanticRuleV1")
  }
}

class SyntacticRuleV1 extends v1.SyntacticRule("SyntacticRuleV1") {
  override def fix(implicit doc: v1.SyntacticDocument): Patch = {
    Patch.addRight(doc.tree, "\nobject SyntacticRuleV1")
  }
}

class CommentFileNonAtomic extends v1.SyntacticRule("CommentFileNonAtomic") {
  override def fix(implicit doc: v1.SyntacticDocument): Patch = {
    Patch.addLeft(doc.tree, "/*") +
      Patch.addRight(doc.tree, "*/")
  }
}

class CommentFileAtomic extends v1.SyntacticRule("CommentFileAtomic") {
  override def fix(implicit doc: v1.SyntacticDocument): Patch = {
    (Patch.addLeft(doc.tree, "/*") +
      Patch.addRight(doc.tree, "*/")).atomic
  }
}
