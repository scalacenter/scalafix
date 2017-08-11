package scalafix.reflect

import scalafix.SemanticCtx
import scalafix.Rewrite
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazyMirror(_ => None)

  def semantic(mirror: SemanticCtx): ConfDecoder[Rewrite] =
    fromLazyMirror(_ => Some(mirror))

  def fromLazyMirror(mirror: LazyMirror): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(mirror),
        baseRewriteDecoders(mirror)
      )
    )
}
