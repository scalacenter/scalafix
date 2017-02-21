package scalafix.rewrite
import scala.meta.Tree
import scala.meta.tokens.Tokens
import scalafix.ScalafixConfig
import scalafix.util.AssociatedComments
import scalafix.util.TokenList

class RewriteCtx[T](implicit val tree: Tree,
                    val config: ScalafixConfig,
                    val mirror: T) {
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  implicit lazy val tokenList: TokenList = new TokenList(tokens)
  implicit lazy val comments: AssociatedComments = AssociatedComments(tokens)
}

object RewriteCtx {
  def apply[T](tree: Tree, config: ScalafixConfig, mirror: T): RewriteCtx[T] =
    new RewriteCtx()(tree, config, mirror)
}
