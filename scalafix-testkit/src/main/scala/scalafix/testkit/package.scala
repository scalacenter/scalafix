package scalafix

package object testkit {
  @deprecated("Renamed to SemanticRuleSuite", "0.5.0")
  type SemanticRewriteSuite = SemanticRuleSuite
  @deprecated("Renamed to SemanticRuleSuite", "0.5.0")
  val SemanticRewriteSuite = SemanticRuleSuite
}
