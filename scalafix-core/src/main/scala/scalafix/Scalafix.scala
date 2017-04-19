package scalafix

import scala.meta._
import scala.util.control.NonFatal
import scalafix.config.ScalafixConfig

object Scalafix {
  def syntaxFix(input: Input,
                config: ScalafixConfig,
                rewrites: Iterable[SyntaxRewrite]): Fixed =
    config.dialect(input).parse[Source] match {
      case parsers.Parsed.Success(ast) =>
        fix(RewriteCtx.syntactic(ast, config), rewrites)
      case parsers.Parsed.Error(pos, msg, details) =>
        Fixed.Failed(Failure.ParseError(pos, msg, details))
    }

  def fix[T](ctx: RewriteCtx[T], rewrites: Iterable[Rewrite[T]]): Fixed =
    try {
      val combinedRewrite = rewrites.foldLeft(Rewrite.empty[T])(_ andThen _)
      Fixed.Success(
        combinedRewrite
          .rewrite(ctx)
          .applied
      ) // (CanOrganizeImports.ScalafixMirror, ctx))
    } catch {
      case NonFatal(e) =>
        Fixed.Failed(Failure.Unexpected(e))
    }

}
