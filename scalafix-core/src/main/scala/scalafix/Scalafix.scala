package scalafix

import scala.meta._
import scala.util.control.NonFatal
import scalafix.config.ScalafixConfig

object Scalafix {
  def fix(input: Input, config: ScalafixConfig): Fixed =
    config.dialect(input).parse[Source] match {
      case parsers.Parsed.Success(ast) =>
        fix(RewriteCtx.syntactic(ast, config))
      case parsers.Parsed.Error(pos, msg, details) =>
        Fixed.Failed(Failure.ParseError(pos, msg, details))
    }

  def fix(ctx: RewriteCtx): Fixed =
    try {
      Fixed.Success(ctx.config.rewrite.wrappedRewrite(ctx).applied)
    } catch {
      case NonFatal(e) =>
        Fixed.Failed(Failure.Unexpected(e))
    }

}
