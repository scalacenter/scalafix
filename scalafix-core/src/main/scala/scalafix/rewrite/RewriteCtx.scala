package scalafix.rewrite
import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.tokens.Tokens
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.util.AssociatedComments
import scalafix.util.TokenList

/** Bundle of useful things when implementing [[Rewrite]].
  *
  * @tparam A The api needed for this Rewrite. Example values:
  *           [[ScalafixMirror]] when using scalafix-nsc,
  *           [[scala.meta.Mirror]] when using scalahost or
  *           [[None]] for syntactic rewrites (i.e., don't care).
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

  /** Constructor for a syntactic rewrite.
    *
    * Syntactic rewrite ctx is RewriteCtx[Null] because it is a subtype of any
    * other rewritectx.
    */
  def apply(tree: Tree,
            config: ScalafixConfig = ScalafixConfig()): RewriteCtx[Null] =
    apply(tree, config, null)

  /** Constructor for a generic rewrite. */
  def apply[T](tree: Tree, config: ScalafixConfig, mirror: T): RewriteCtx[T] =
    new RewriteCtx()(tree, config, mirror)
}
