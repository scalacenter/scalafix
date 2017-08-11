package scalafix.reflect

import scalafix.SemanticCtx
import scalafix.Rewrite
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazySemanticCtx(_ => None)

  def semantic(semanticCtx: SemanticCtx): ConfDecoder[Rewrite] =
    fromLazySemanticCtx(_ => Some(semanticCtx))

  def fromLazySemanticCtx(semanticCtx: LazySemanticCtx): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(semanticCtx),
        baseRewriteDecoders(semanticCtx)
      )
    )
}
