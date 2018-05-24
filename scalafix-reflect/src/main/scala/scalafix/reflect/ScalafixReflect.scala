package scalafix.reflect

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import scalafix.Rule
import scalafix.SemanticdbIndex
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import scalafix.internal.util.ClassloadRule
import scalafix.internal.v1.LegacySemanticRule
import scalafix.internal.v1.LegacySyntacticRule
import scalafix.internal.v1.Rules
import scalafix.patch.TreePatch
import scalafix.v1

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
