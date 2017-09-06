package scalafix.reflect

import scalafix.SemanticdbIndex
import scalafix.Rule
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex.empty)

  def semantic(sctx: SemanticdbIndex): ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex(_ => Some(sctx)))

  def fromLazySemanticdbIndex(sctx: LazySemanticdbIndex): ConfDecoder[Rule] =
    ruleConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(sctx),
        baseRuleDecoders(sctx)
      )
    )
}
