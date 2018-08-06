package scalafix.reflect

import metaconfig.ConfDecoder
import scalafix.v0

object ScalafixReflect {
  @deprecated("Use scalafix.v1.RuleDecoder.decoder() instead", "0.6.0")
  def syntactic: ConfDecoder[v0.Rule] =
    throw new UnsupportedOperationException

  @deprecated("Use scalafix.v1.RuleDecoder.decoder() instead", "0.6.0")
  def semantic(index: v0.SemanticdbIndex): ConfDecoder[v0.Rule] =
    throw new UnsupportedOperationException
}
