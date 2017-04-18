package scalafix

import scala.util.control.NonFatal
import scalafix.rewrite.ScalafixMirror
import scalafix.util.CanOrganizeImports

object Scalafix {
  def fix(ctx: RewriteCtx[ScalafixMirror]): Fixed =
    try {
      Fixed.Success(
        ctx.config.combinedRewrite
          .rewrite(ctx)
          .applied(CanOrganizeImports.ScalafixMirror, ctx))
    } catch {
      case NonFatal(e) =>
        Fixed.Failed(Failure.Unexpected(e))
    }

}
