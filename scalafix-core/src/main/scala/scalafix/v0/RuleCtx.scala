package scalafix.v0

import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.rule.RuleCtxImpl
import scalafix.patch.PatchOps
import scalafix.util.MatchingParens
import scalafix.util.TokenList
import org.scalameta.FileLine
import scalafix.internal.patch.EscapeHatch

trait RuleCtx extends PatchOps {

  /** The parsed syntax tree that should be fixed.
    *
    * The scalafix API does not support fixing un-parseable code at this point.
    */
  def tree: Tree

  /** The input where the tree was parsed from.
    *
    * This is typically either Input.VirtualFile for semantic rules
    * and Input.File for syntactic rules. For Input.VirtualFile, it is
    * possible to trace back to the original file path via SemanticdbIndex.sourcepath.
    */
  def input: Input

  /** The tokenized tokens of this this tree. **/
  def tokens: Tokens

  /** Find matching open/close pairs of parens/braces/brackets. **/
  def matchingParens: MatchingParens

  /** Traverse tokens as a doubly linked list. **/
  def tokenList: TokenList

  /** Find comments/docstrings associated with tree nodes. **/
  def comments: AssociatedComments

  /** Get SemanticdbIndex for this single tree alone. */
  def index(implicit index: SemanticdbIndex): SemanticdbIndex

  /** Print out the contents of SemanticdbIndex for this input only. **/
  def debugIndex()(implicit index: SemanticdbIndex, fileLine: FileLine): Unit

  // Private scalafix methods, subject for removal without notice.
  private[scalafix] def toks(t: Tree): Tokens
  private[scalafix] def config: ScalafixConfig
  private[scalafix] def escapeHatch: EscapeHatch
  private[scalafix] def diffDisable: DiffDisable
}

object RuleCtx {
  def apply(tree: Tree): RuleCtx =
    apply(tree, ScalafixConfig.default, DiffDisable.empty)

  private[scalafix] def apply(tree: Tree, config: ScalafixConfig): RuleCtx =
    apply(tree, config, DiffDisable.empty)

  private[scalafix] def apply(
      tree: Tree,
      config: ScalafixConfig,
      diffDisable: DiffDisable): RuleCtx =
    RuleCtxImpl(tree, config, diffDisable)
}
