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

/** Bundle of useful things when implementing [[Rewrite]].
  *
  * @tparam A The api needed for this Rewrite. Example values:
  *           [[ScalafixMirror]] when using scalafix-nsc,
  *           [[scala.meta.Mirror]] when using scalahost or open
  *           type parameter for syntactic rewrites (that is, no type bounds).
  */
class RewriteCtx[+A](implicit val tree: Tree,
                     val config: ScalafixConfig,
                     val mirror: A) {
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = new TokenList(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  val reporter: ScalafixReporter = config.reporter
  def map[B](f: A => B): RewriteCtx[B] =
    new RewriteCtx[B]()(tree, config, f(mirror))
}

object RewriteCtx {
  private lazy val syntacticRewriteCtx: Any = new Object()

  def syntactic(
      tree: Tree,
      config: ScalafixConfig = ScalafixConfig()): SyntacticRewriteCtx =
    apply(tree, config, syntacticRewriteCtx)

  def semantic(tree: Tree,
               mirror: Mirror,
               config: ScalafixConfig = ScalafixConfig()): SemanticRewriteCtx =
    apply(tree, config, mirror)

  /** Constructor for a generic rewrite. */
  def apply[T](tree: Tree, config: ScalafixConfig, mirror: T): RewriteCtx[T] =
    new RewriteCtx()(tree, config, mirror)
}
