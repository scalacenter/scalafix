package scalafix.internal.patch

import org.scalameta.FileLine
import scala.meta.Input
import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.v0.RuleCtx
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
    index
  override def debugIndex()(
      implicit index: SemanticdbIndex,
      fileLine: FileLine): Unit =
    throw new UnsupportedOperationException
  override private[scalafix] def toks(t: Tree) = t.tokens(doc.config.dialect)
  override private[scalafix] def config = doc.config
  override private[scalafix] def escapeHatch = doc.escapeHatch
  override private[scalafix] def diffDisable = doc.diffDisable
}
