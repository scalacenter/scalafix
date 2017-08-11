package scalafix

import scalafix.internal.config.LazyMirror
import metaconfig.ConfDecoder
import metaconfig.Configured
import scala.meta.Input

package object config {
  def fromString(
      configuration: String,
      decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    fromInput(Input.String(configuration), _ => None, Nil, decoder)

  /** Load configuration from an input.
    *
    * @param configuration Contents to parse into configuration. Most typically a
    *                      [[scala.meta.Input.File]] or [[scala.meta.Input.String]].
    * @param semanticCtx Callback to build semantic context for semantic rewrites.
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
      semanticCtx: LazyMirror,
      extraRewrites: List[String],
      rewriteDecoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    ScalafixConfig.fromInput(configuration, semanticCtx, extraRewrites)(
      rewriteDecoder)
}
