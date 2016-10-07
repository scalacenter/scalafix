package scalafix

import scala.meta._
import scala.meta.inputs.Input
import scalafix.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.util.Patch
import scalafix.util.TokenList

object Scalafix {
  def fix(code: String, config: ScalafixConfig = ScalafixConfig()): Fixed = {
    fix(Input.String(code), config)
  }

  def fix(code: Input, config: ScalafixConfig): Fixed = {
    config.parser.apply(code, config.dialect) match {
      case Parsed.Success(ast) =>
        val m = Mirror(
          config.project.targetFiles.map(x => Artifact(x.getAbsolutePath)): _*)
        val ctx = RewriteCtx(
          config,
          new TokenList(ast.tokens),
          Some(m)
        )
        val patches: Seq[Patch] = config.rewrites.flatMap(_.rewrite(ast, ctx))
        Right(Patch.apply(ast.tokens, patches))
      case Parsed.Error(pos, msg, e) =>
        Left(Failure.ParseError(pos, msg, e))
    }
  }
}
