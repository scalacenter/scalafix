package scalafix.v1

import scala.meta.Input
import scala.meta.Tokens
import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch
import scalafix.util.MatchingParens
import scalafix.util.TokenList

final class Doc private[scalafix] (
    val tree: Tree,
    val tokens: Tokens,
    val input: Input,
    val matchingParens: MatchingParens,
    val tokenList: TokenList,
    val comments: AssociatedComments,
    // privates
    private[scalafix] val config: ScalafixConfig,
    private[scalafix] val escapeHatch: EscapeHatch,
    private[scalafix] val diffDisable: DiffDisable
) {
  def toks(tree: Tree): Tokens = tree.tokens(config.dialect)
}
object Doc {
  def apply(tree: Tree): Doc = {
    Doc(tree, DiffDisable.empty, ScalafixConfig.default)
  }
  def apply(
      tree: Tree,
      diffDisable: DiffDisable,
      config: ScalafixConfig): Doc = {
    val tokens = tree.tokens
    val input = tokens.headOption match {
      case Some(token) => token.input
      case _ => Input.None
    }
    val comments = AssociatedComments(tokens)
    val escape = EscapeHatch(tree, comments)
    new Doc(
      tree = tree,
      tokens = tokens,
      input = input,
      matchingParens = MatchingParens(tokens),
      tokenList = TokenList(tokens),
      comments,
      config,
      escape,
      diffDisable
    )
  }
}
