package scalafix.reflect

import scalafix.SemanticCtx
import scalafix.Rewrite
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazyMirror(_ => None)

  def semantic(semanticCtx: SemanticCtx): ConfDecoder[Rewrite] =
    fromLazyMirror(_ => Some(semanticCtx))

  def fromLazyMirror(semanticCtx: LazyMirror): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(semanticCtx),
        baseRewriteDecoders(semanticCtx)
      )
    )
}
