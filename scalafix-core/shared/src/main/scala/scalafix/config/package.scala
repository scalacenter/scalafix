package scalafix

import scalafix.internal.config.LazySemanticCtx
import metaconfig.ConfDecoder
import metaconfig.Configured
import scala.meta.Input

package object config {
  @deprecated(
    "ScalafixConfig is now internal, import scalafix.internal.config.ScalafixConfig instead.",
    "0.5.0")
  type ScalafixConfig = internal.config.ScalafixConfig
  @deprecated(
    "ScalafixConfig is now internal, import scalafix.internal.config.ScalafixConfig instead.",
    "0.5.0")
  val ScalafixConfig = internal.config.ScalafixConfig
  def fromString(
      configuration: String,
      decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, internal.config.ScalafixConfig)] =
    fromInput(Input.String(configuration), LazySemanticCtx.empty, Nil, decoder)

  /** Load configuration from an input.
    *
    * @param configuration Contents to parse into configuration. Most typically a
    *                      [[scala.meta.Input.File]] or [[scala.meta.Input.String]].
    * @param sctx Callback to build semantic context for semantic rewrites.
    * @param extraRewrites additional rewrites to load on top of those defined
    *                      in the .scalafix.conf configuration file.
    * @param rewriteDecoder the decoder for parsing `rewrite` fields.
    *                       - Most basic: [[scalafix.Rewrite.syntaxRewriteConfDecoder]]
    *                       - With support for compilation from rewrites, see
    *                         scalafix.reflect.ScalafixReflect (separate module).
    * @return
    */
  def fromInput(
      configuration: Input,
      sctx: LazySemanticCtx,
      extraRewrites: List[String],
      rewriteDecoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, internal.config.ScalafixConfig)] =
    internal.config.ScalafixConfig
      .fromInput(configuration, sctx, extraRewrites)(rewriteDecoder)
}
