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

  type Diagnostic = scalafix.lint.Diagnostic
  val Diagnostic = scalafix.lint.Diagnostic

  type CustomMessage[T] = scalafix.config.CustomMessage[T]
  val CustomMessage = scalafix.config.CustomMessage

  implicit class XtensionScalafixTreeInspect(tree: Tree) {
    def inspect: String =
      Inspect.prettyTree(tree, showFieldNames = false).render(1)
    def inspectLabeled: String =
      Inspect.prettyTree(tree, showFieldNames = true).render(1)
  }

  implicit class XtensionScalafixOptionTreeInspect(tree: Option[Tree]) {
    def inspect: String =
      Inspect.prettyOption(tree, showFieldNames = false).render(1)
    def inspectLabeled: String =
      Inspect.prettyOption(tree, showFieldNames = true).render(1)
  }

  implicit class XtensionScalafixListTreeInspect(tree: List[Tree]) {
    def inspect: String =
      Inspect.prettyList(tree, showFieldNames = false).render(1)
    def inspectLabeled: String =
      Inspect.prettyList(tree, showFieldNames = true).render(1)
  }

}
