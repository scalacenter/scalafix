package scalafix
package rewrite
import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scala.meta.inputs.Input
import scala.meta.io.AbsolutePath
import scala.meta.tokens.Tokens
import scalafix.syntax._
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.util.TokenList

/** Bundle of useful things when implementing [[Rewrite]]. */
case class RewriteCtx(tree: Tree, config: ScalafixConfig) {
  def toks(t: Tree): Tokens = t.tokens(config.dialect)
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = new TokenList(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  val reporter: ScalafixReporter = config.reporter
}
