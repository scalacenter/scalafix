package scalafix

import scala.meta.Term
import scala.meta.Tree
import scala.meta.inputs.Position
import scalafix.internal.util.PositionSyntax
import scalafix.util.Api

package object v1 extends Api {
  implicit class XtensionTreeScalafix(tree: Tree) {
    def symbol(implicit doc: SemanticDocument): Symbol =
      doc.internal.symbol(tree)
  }
  implicit class XtensionTermScalafix(term: Term) {
    def synthetic(implicit doc: SemanticDocument): Option[SemanticTree] =
      doc.internal.synthetic(term.pos)
  }
  implicit class XtensionTermInfixScalafix(infix: Term.ApplyInfix) {
    def syntheticOperator(
        implicit doc: SemanticDocument
    ): Option[SemanticTree] = {
      val operatorPosition =
        Position.Range(infix.pos.input, infix.pos.start, infix.op.pos.end)
      doc.internal.synthetic(operatorPosition)
    }
  }
  implicit class XtensionPositionScalafix(pos: Position) {
    def formatMessage(severity: String, message: String): String = {
      PositionSyntax.formatMessage(pos, severity, message)
    }
  }

  implicit class XtensionSemanticTree(tree: SemanticTree) {
    def symbol(implicit sdoc: SemanticDocument): Option[Symbol] = tree match {
      case t: ApplyTree =>
        t.function.symbol
      case t: TypeApplyTree =>
        t.function.symbol
      case t: SelectTree =>
        Some(t.id.info.symbol)
      case t: IdTree =>
        Some(t.info.symbol)
      case t: OriginalTree =>
        t.tree.symbol.asNonEmpty
      case t: OriginalSubTree =>
        t.tree.symbol.asNonEmpty
      case _: MacroExpansionTree | _: LiteralTree | _: FunctionTree | NoTree =>
        None
    }
  }
}
