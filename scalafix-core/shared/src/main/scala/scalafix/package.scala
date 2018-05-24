package object scalafix {

  type SemanticdbIndex = scalafix.util.SemanticdbIndex
  val SemanticdbIndex = scalafix.util.SemanticdbIndex

  type RuleCtx = rule.RuleCtx
  val RuleCtx = rule.RuleCtx

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

  type SemanticRule = rule.SemanticRule
  type Rule = rule.Rule
  val Rule = rule.Rule

  type Patch = patch.Patch
  val Patch = patch.Patch

  type LintCategory = scalafix.lint.LintCategory
  val LintCategory = scalafix.lint.LintCategory

  type LintMessage = scalafix.lint.LintMessage

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }

  private[scalafix] implicit class XtensionString(val value: String)
      extends AnyVal {
    def isMultiline: Boolean =
      value.indexOf('\n') != -1
  }

  // inlined langmeta APIs

  type Database = scalafix.v0.Database
  val Database = scalafix.v0.Database

  type Document = scalafix.v0.Document
  val Document = scalafix.v0.Document

  type Synthetic = scalafix.v0.Synthetic
  val Synthetic = scalafix.v0.Synthetic

  type Message = scalafix.v0.Message
  val Message = scalafix.v0.Message

  type Severity = scalafix.v0.Severity
  val Severity = scalafix.v0.Severity

  type Denotation = scalafix.v0.Denotation
  val Denotation = scalafix.v0.Denotation

  type ResolvedName = scalafix.v0.ResolvedName
  val ResolvedName = scalafix.v0.ResolvedName

  type ResolvedSymbol = scalafix.v0.ResolvedSymbol
  val ResolvedSymbol = scalafix.v0.ResolvedSymbol
}
