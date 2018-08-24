import scalafix.util.Api
package object scalafix extends Api {

  private final val DeprecationMessage =
    "import scalafix.v0._ instead of import scalafix._"

  @deprecated(DeprecationMessage, "0.6.0")
  type SemanticdbIndex = scalafix.util.SemanticdbIndex
  @deprecated(DeprecationMessage, "0.6.0")
  val SemanticdbIndex = scalafix.util.SemanticdbIndex

  @deprecated(DeprecationMessage, "0.6.0")
  type RuleCtx = rule.RuleCtx
  @deprecated(DeprecationMessage, "0.6.0")
  val RuleCtx = rule.RuleCtx

  @deprecated(DeprecationMessage, "0.6.0")
  type SemanticRule = rule.SemanticRule
  @deprecated(DeprecationMessage, "0.6.0")
  type Rule = rule.Rule
  @deprecated(DeprecationMessage, "0.6.0")
  val Rule = rule.Rule

  // inlined langmeta APIs

  @deprecated(DeprecationMessage, "0.6.0")
  type Database = scalafix.v0.Database
  @deprecated(DeprecationMessage, "0.6.0")
  val Database = scalafix.v0.Database

  @deprecated(DeprecationMessage, "0.6.0")
  type Document = scalafix.v0.Document
  @deprecated(DeprecationMessage, "0.6.0")
  val Document = scalafix.v0.Document

  @deprecated(DeprecationMessage, "0.6.0")
  type Synthetic = scalafix.v0.Synthetic
  @deprecated(DeprecationMessage, "0.6.0")
  val Synthetic = scalafix.v0.Synthetic

  @deprecated(DeprecationMessage, "0.6.0")
  type Message = scalafix.v0.Message
  @deprecated(DeprecationMessage, "0.6.0")
  val Message = scalafix.v0.Message

  @deprecated(DeprecationMessage, "0.6.0")
  type Severity = scalafix.v0.Severity
  @deprecated(DeprecationMessage, "0.6.0")
  val Severity = scalafix.v0.Severity

  @deprecated(DeprecationMessage, "0.6.0")
  type Denotation = scalafix.v0.Denotation
  @deprecated(DeprecationMessage, "0.6.0")
  val Denotation = scalafix.v0.Denotation

  @deprecated(DeprecationMessage, "0.6.0")
  type ResolvedName = scalafix.v0.ResolvedName
  @deprecated(DeprecationMessage, "0.6.0")
  val ResolvedName = scalafix.v0.ResolvedName

  @deprecated(DeprecationMessage, "0.6.0")
  type ResolvedSymbol = scalafix.v0.ResolvedSymbol
  @deprecated(DeprecationMessage, "0.6.0")
  val ResolvedSymbol = scalafix.v0.ResolvedSymbol
}
