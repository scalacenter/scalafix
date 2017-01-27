package scalafix.rewrite
import scalafix.ScalafixConfig
import scalafix.util.AssociatedComments
import scalafix.util.TokenList

case class RewriteCtx(
    config: ScalafixConfig,
    tokenList: TokenList,
    comments: AssociatedComments,
    semantic: Option[SemanticApi]
)
