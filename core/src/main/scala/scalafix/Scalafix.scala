package scalafix

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.parsers.Parsed
import scalafix.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.ScalafixCtx
import scalafix.rewrite.ScalafixMirror
import scalafix.util.CanOrganizeImports
import scalafix.util.Patch

object Scalafix {
  def fix(code: Input,
          config: ScalafixConfig,
          semanticApi: Option[ScalafixMirror]): Fixed = {
    config.parser.apply(code, config.dialect) match {
      case Parsed.Success(ast) =>
        fix(ast, config, semanticApi)
      case Parsed.Error(pos, msg, e) =>
        Fixed.Failed(Failure.ParseError(pos, msg, e))
    }
  }
  def fix(ast: Tree,
          config: ScalafixConfig,
          semanticApi: Option[ScalafixMirror]): Fixed = {
    implicit val ctx: ScalafixCtx = RewriteCtx(
      ast,
      config,
      semanticApi.getOrElse(ScalafixMirror.empty(config.dialect)))
    val patches = config.rewrites.flatMap(_.rewrite(ctx))
    Fixed.Success(Patch.apply(patches)(CanOrganizeImports.ScalafixMirror, ctx))
  }

  def fix(code: Input, config: ScalafixConfig): Fixed = {
    fix(code, config, None)
  }

  def fix(code: String, config: ScalafixConfig = ScalafixConfig()): Fixed = {
    fix(Input.String(code), config, None)
  }
}
