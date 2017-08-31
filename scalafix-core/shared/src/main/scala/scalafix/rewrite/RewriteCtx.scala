package scalafix
package rewrite
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.rewrite.RewriteCtxImpl
import scalafix.patch.PatchOps
import scalafix.util.MatchingParens
import scalafix.util.SemanticCtx
import scalafix.util.TokenList
import org.scalameta.FileLine

trait RewriteCtx extends PatchOps {

  /** The parsed syntax tree that should be fixed.
    *
    * The scalafix API does not support fixing un-parseable code at this point.
    */
  def tree: Tree

  /** The input where the tree was parsed from.
    *
    * This is typically either Input.VirtualFile for semantic rewrites
    * and Input.File for syntactic rewrites. For Input.VirtualFile, it is
    * possible to trace back to the original file path via SemanticCtx.sourcepath.
    */
  def input: Input

  /** The tokenized tokens of this this tree. **/
  def tokens: Tokens

  /** Find matching open/close pairs of parens/braces/brackets. **/
  def matchingParens: MatchingParens

  @deprecated("Renamed to matchingParens", "0.5.0")
  def matching: MatchingParens = matchingParens

  /** Traverse tokens as a doubly linked list. **/
  def tokenList: TokenList

  /** Find comments/docstrings associated with tree nodes. **/
  def comments: AssociatedComments

  /** Get SemanticCtx for this single tree alone. */
  def sctx(implicit sctx: SemanticCtx): SemanticCtx

  /** Print out the contents of SemanticCtx for this input only. **/
  def debugSemanticCtx()(implicit sctx: SemanticCtx, fileLine: FileLine): Unit

  // Private scalafix methods, subject for removal without notice.
  private[scalafix] def toks(t: Tree): Tokens
  private[scalafix] def config: ScalafixConfig
  private[scalafix] def printLintMessage(
      msg: LintMessage,
      owner: RewriteName): Unit
}

object RewriteCtx {
  def apply(tree: Tree): RewriteCtx =
    apply(tree, ScalafixConfig.default)
  private[scalafix] def apply(tree: Tree, config: ScalafixConfig): RewriteCtx =
    RewriteCtxImpl(tree, config)
}
