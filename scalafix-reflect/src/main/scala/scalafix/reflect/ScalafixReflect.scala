package scalafix.reflect

import scalafix.SemanticCtx
import scalafix.Rewrite
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazySemanticCtx(LazySemanticCtx.empty)

  def semantic(sctx: SemanticCtx): ConfDecoder[Rewrite] =
    fromLazySemanticCtx(LazySemanticCtx(_ => Some(sctx)))

  def fromLazySemanticCtx(sctx: LazySemanticCtx): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(sctx),
        baseRewriteDecoders(sctx)
      )
    )
}
