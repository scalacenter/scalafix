package scalafix

import scala.meta.Tree
import scalafix.internal.util.ScalafixSyntax

package object v1 extends ScalafixSyntax {
  implicit class XtensionTreeSymbol(tree: Tree) {
    def symbol(implicit doc: SemanticDoc): Symbol = doc.internal.symbol(tree)
  }
}
