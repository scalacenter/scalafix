package scalafix.util

import scala.meta.Tree
import scalafix.internal.util.Inspect

trait Api {

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

  implicit class XtensionScalafixTreeInspect(tree: Tree) {
    def inspect: String = Inspect.pretty(tree, showFieldNames = false)
    def inspectLabeled: String = Inspect.pretty(tree, showFieldNames = true)
  }

  type Diagnostic = scalafix.lint.Diagnostic
  val Diagnostic = scalafix.lint.Diagnostic

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

}
