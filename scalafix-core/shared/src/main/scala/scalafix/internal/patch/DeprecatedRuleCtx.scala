package scalafix.internal.patch

import org.scalameta.FileLine
import scala.meta.Input
import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.LintMessage
import scalafix.Patch
import scalafix.RuleCtx
import scalafix.rule.RuleName
import scalafix.util.MatchingParens
import scalafix.util.SemanticdbIndex
import scalafix.util.TokenList
import scalafix.v1.Doc

class DeprecatedRuleCtx(doc: Doc) extends RuleCtx with DeprecatedPatchOps {
  override def tree: Tree = doc.tree
  override def input: Input = doc.input
  override def tokens: Tokens = doc.tokens
  override def matchingParens: MatchingParens = doc.matchingParens
  override def tokenList: TokenList = doc.tokenList
  override def comments: AssociatedComments = doc.comments
  override def index(implicit index: SemanticdbIndex): SemanticdbIndex =
    throw new UnsupportedOperationException
  override def debugIndex()(
      implicit index: SemanticdbIndex,
      fileLine: FileLine): Unit =
    throw new UnsupportedOperationException
  override private[scalafix] def toks(t: Tree) = t.tokens(doc.config.dialect)
  override private[scalafix] def config = doc.config
  override private[scalafix] def printLintMessage(msg: LintMessage): Unit = {
    // Copy-paste from RuleCtxImpl :(
    val category = msg.category.withConfig(config.lint)
    config.lint.reporter.handleMessage(
      msg.format(config.lint.explain),
      msg.position,
      category.severity.toSeverity
    )
  }
  override private[scalafix] def filter(
      patchesByName: Map[RuleName, Patch],
      index: SemanticdbIndex) =
    doc.escapeHatch.filter(patchesByName, this, index, doc.diffDisable)
}
