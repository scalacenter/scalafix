package scalafix.reflect

import scalafix.SemanticCtx
import scalafix.Rule
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rule] =
    fromLazySemanticCtx(LazySemanticCtx.empty)

  def semantic(sctx: SemanticCtx): ConfDecoder[Rule] =
    fromLazySemanticCtx(LazySemanticCtx(_ => Some(sctx)))

  def fromLazySemanticCtx(sctx: LazySemanticCtx): ConfDecoder[Rule] =
    ruleConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(sctx),
        baseRuleDecoders(sctx)
      )
    )
}
