package scalafix.reflect

import scalafix.SemanticdbIndex
import scalafix.Rule
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex.empty)

  def semantic(index: SemanticdbIndex): ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex(_ => Some(index)))

  def fromLazySemanticdbIndex(index: LazySemanticdbIndex): ConfDecoder[Rule] =
    ruleConfDecoder(
      ScalafixCompilerDecoder
        .baseCompilerDecoder(index)
        .orElse(baseRuleDecoders(index))
    )
}
