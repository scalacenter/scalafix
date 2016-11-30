package scalafix

import scala.meta._
import scala.meta.inputs.Input
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.SemanticApi
import scalafix.util.Patch
import scalafix.util.TokenList

object Scalafix {
  def fix(code: Input,
          config: ScalafixConfig,
          semanticApi: Option[SemanticApi]): Fixed = {
    config.parser.apply(code, config.dialect) match {
      case Parsed.Success(ast) =>
        val ctx = RewriteCtx(config, new TokenList(ast.tokens), semanticApi)
        val patches: Seq[Patch] = config.rewrites.flatMap(_.rewrite(ast, ctx))
        Fixed.Success(Patch.apply(ast.tokens, patches))
      case Parsed.Error(pos, msg, e) =>
        Fixed.Failed(Failure.ParseError(pos, msg, e))
    }
  }

  def fix(code: Input, config: ScalafixConfig): Fixed = {
    fix(code, config, None)
  }

  def fix(code: String, config: ScalafixConfig = ScalafixConfig()): Fixed = {
    fix(Input.String(code), config, None)
  }
}
