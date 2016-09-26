package scalafix.rewrite

import scalafix.ScalafixConfig
import scalafix.util.TokenList

case class RewriteCtx(
    config: ScalafixConfig,
    tokenList: TokenList
)
