package scalafix

import scala.meta._
import scala.util.control.NonFatal

object Scalafix {
  def fix(input: String, config: ScalafixConfig): Fixed =
    fix(Input.String(input), config)
  def fix(input: Input, config: ScalafixConfig): Fixed =
    config.dialect(input).parse[Source] match {
      case parsers.Parsed.Success(ast) =>
        fix(RewriteCtx(ast, config))
      case parsers.Parsed.Error(pos, msg, details) =>
        Fixed.Failed(Failure.ParseError(pos, msg, details))
    }

  def fix(ctx: RewriteCtx): Fixed =
    try {
      Fixed.Success(ctx.config.rewrite.apply(ctx))
    } catch {
      case NonFatal(e) =>
        Fixed.Failed(Failure.Unexpected(e))
    }

}
