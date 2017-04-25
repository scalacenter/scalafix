package scalafix
package rewrite
import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.semantic.v1.Mirror
import scala.meta.tokens.Tokens
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.util.AssociatedComments
import scalafix.util.TokenList

/** Bundle of useful things when implementing [[Rewrite]]. */
class RewriteCtx(implicit val tree: Tree, val config: ScalafixConfig) {
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = new TokenList(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  val reporter: ScalafixReporter = config.reporter
}

object RewriteCtx {
  private lazy val syntacticRewriteCtx: Any = new Object()

  def syntactic(tree: Tree,
                config: ScalafixConfig = ScalafixConfig()): RewriteCtx =
    apply(tree, config)

  def semantic(tree: Tree,
               config: ScalafixConfig = ScalafixConfig()): RewriteCtx =
    apply(tree, config)

  /** Constructor for a generic rewrite. */
  def apply(tree: Tree, config: ScalafixConfig): RewriteCtx =
    new RewriteCtx()(tree, config)
}
