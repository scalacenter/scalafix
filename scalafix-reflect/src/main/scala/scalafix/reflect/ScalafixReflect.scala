package scalafix.reflect

import metaconfig.ConfDecoder
import scalafix.Rule
import scalafix.SemanticdbIndex
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder

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
