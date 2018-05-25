package scalafix.reflect

import metaconfig.ConfDecoder
import scalafix.Rule
import scalafix.SemanticdbIndex
import scalafix.internal.config._

object ScalafixReflect {
  @deprecated("Use scalafix.v1.RuleDecoder.decoder() instead", "0.6.0")
  def syntactic: ConfDecoder[Rule] =
    throw new UnsupportedOperationException

  @deprecated("Use scalafix.v1.RuleDecoder.decoder() instead", "0.6.0")
  def semantic(index: SemanticdbIndex): ConfDecoder[Rule] =
    throw new UnsupportedOperationException

  @deprecated("Use scalafix.v1.RuleDecoder.decoder() instead", "0.6.0")
  def fromLazySemanticdbIndex(index: LazySemanticdbIndex): ConfDecoder[Rule] =
    throw new UnsupportedOperationException
}
