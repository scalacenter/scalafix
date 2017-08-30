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
    * The API does not support fixing un-parseable code at this point.
    */
  def tree: Tree

  /** The input where the tree was parsed from.
    *
    * This is typically either Input.VirtualFile for semantic rewrites
    * and Input.File for syntactic rewrites. For Input.VirtualFile, it is
    * possible to trace back to the original file path via SemanticCtx.sourceroot.
    */
  def input: Input
  def tokens: Tokens
  def matching: MatchingParens
  def tokenList: TokenList
  def comments: AssociatedComments
  def debugSemanticCtx()(implicit sctx: SemanticCtx, fileLine: FileLine): Unit
  def debug(values: sourcecode.Text[Any]*)(implicit fileLine: FileLine): Unit

  // Private scalafix methods, may be removed without notice.
  private[scalafix] def toks(t: Tree): Tokens
  private[scalafix] def config: ScalafixConfig
  private[scalafix] def printLintMessage(
      msg: LintMessage,
      owner: RewriteName): Unit
}

object RewriteCtx {
  def apply(tree: Tree, config: ScalafixConfig): RewriteCtx =
    RewriteCtxImpl(tree, config)
}
