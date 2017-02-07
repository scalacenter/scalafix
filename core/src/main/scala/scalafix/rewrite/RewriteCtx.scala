package scalafix.rewrite
import scala.meta.Tree
import scala.meta.tokens.Tokens
import scalafix.ScalafixConfig
import scalafix.util.AssociatedComments
import scalafix.util.TokenList

case class RewriteCtx(
    config: ScalafixConfig,
    tokens: Tokens,
    tokenList: TokenList,
    comments: AssociatedComments,
    semantic: Option[ScalafixMirror]
)

object RewriteCtx {
  def fromCode(ast: Tree,
               config: ScalafixConfig = ScalafixConfig(),
               semanticApi: Option[ScalafixMirror] = None): RewriteCtx = {
    val tokens = ast.tokens(config.dialect)
    RewriteCtx(
      config,
      tokens,
      new TokenList(tokens),
      AssociatedComments(tokens),
      semanticApi
    )
  }
}
