package scalafix.internal.v1

import scala.meta.Tree
import scala.meta.internal.ScalametaInternals
import scala.meta.internal.{semanticdb => s}
import scalafix.v1._

object DocumentFromProtobuf {
  def convert(synth: s.Synthetic, doc: InternalSemanticDoc): SemanticTree =
    new DocumentFromProtobuf(synth)(new SemanticDocument(doc)).stree(synth.tree)
}
final class DocumentFromProtobuf(original: s.Synthetic)(
    implicit doc: SemanticDocument) {
  val convert = new SymtabFromProtobuf(doc)
  def stree(t: s.Tree): SemanticTree = {
    t match {
      case t: s.ApplyTree =>
        ApplyTree(t.function.convert, t.arguments.convert)
      case t: s.FunctionTree =>
        FunctionTree(t.parameters.convert, t.body.convert)
      case t: s.IdTree =>
        sid(t)
      case t: s.LiteralTree =>
        val const = convert.sconstant(t.constant)
        LiteralTree(const)
      case t: s.MacroExpansionTree =>
        val tpe = convert.stype(t.tpe)
        MacroExpansionTree(t.beforeExpansion.convert, tpe)
      case t: s.OriginalTree =>
        soriginal(t.range) match {
          case Some(tree) =>
            val isOriginal = original.range.exists(t.range.contains)
            if (isOriginal) OriginalTree(tree)
            else OriginalSubTree(tree)
          case None => NoTree
        }
      case t: s.SelectTree =>
        t.id match {
          case Some(id) =>
            SelectTree(t.qualifier.convert, sid(id))
          case None =>
            NoTree
        }
      case t: s.TypeApplyTree =>
        val targs =
          t.typeArguments.iterator.map(tpe => convert.stype(tpe)).toList
        TypeApplyTree(t.function.convert, targs)
      case s.NoTree =>
        NoTree
    }
  }

  private def sid(id: s.IdTree): IdTree =
    IdTree(Symbol(id.symbol))

  private def soriginal(range: Option[s.Range]): Option[Tree] = {
    val pos = ScalametaInternals.positionFromRange(doc.input, range)
    PositionSearch.find(doc.tree, pos)
  }

  private implicit class RichTree(tree: s.Tree) {
    def convert: SemanticTree = stree(tree)
  }
  private implicit class RichIds(ids: Seq[s.IdTree]) {
    def convert: List[IdTree] =
      ids.iterator.map(sid).toList
  }
  private implicit class RichTrees(trees: Seq[s.Tree]) {
    def convert: List[SemanticTree] = trees.iterator.map(stree).toList
  }

}
