package scalafix.rewrite

import scala.{meta => m}
import scalafix.ScalafixConfig
import scalafix.util.TokenList

case class RewriteCtx(
    config: ScalafixConfig,
    tokenList: TokenList,
    semantic: Option[SemanticApi]
)

// The scala.meta semantic api is  not ready yet. For time being, we
// can implement our own simplified version of the semantic api to meet
// scalafix's custom needs.
trait SemanticApi {
  // ExplicitImplicit rewrite.
  def typeSignature(defn: m.Defn): Option[m.Type]
}
