package scalafix.internal.util

trait ScalafixSyntax {

  type RuleName = scalafix.rule.RuleName
  val RuleName = scalafix.rule.RuleName

  type Patch = scalafix.patch.Patch
  val Patch = scalafix.patch.Patch

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }
}
