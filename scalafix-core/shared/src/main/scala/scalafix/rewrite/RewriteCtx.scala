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
  def tree: Tree
  def tokens: Tokens
  def matching: MatchingParens
  def tokenList: TokenList
  def comments: AssociatedComments
  def input: Input
  def debugSemanticCtx()(implicit sctx: SemanticCtx, fileLine: FileLine): Unit
  def debug(values: sourcecode.Text[Any]*)(implicit fileLine: FileLine): Unit
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
