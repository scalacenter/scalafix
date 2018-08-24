package scalafix

import scala.meta.Tree
import scalafix.util.Api
package object v1 extends Api {
  implicit class XtensionTreeSymbol(tree: Tree) {
    def symbol(implicit doc: SemanticDoc): Symbol = doc.internal.symbol(tree)
  }
}
