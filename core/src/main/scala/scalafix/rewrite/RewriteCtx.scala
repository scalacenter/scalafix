package scalafix.rewrite

import scala.meta.Tree
import scala.meta.inputs.Input
import scala.meta.semantic.Mirror
import scalafix.config.ScalafixConfig
import scalafix.util.TokenList

case class RewriteInput(
    input: Input,
    ast: Tree
)

case class RewriteCtx(
    config: ScalafixConfig,
    tokenList: TokenList,
    mirror: Option[Mirror]
)
