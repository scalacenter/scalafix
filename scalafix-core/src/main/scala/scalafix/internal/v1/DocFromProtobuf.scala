package scalafix.internal.v1

import scala.meta.Tree
import scala.meta.internal.ScalametaInternals
import scala.meta.internal.{semanticdb => s}
import scalafix.v1._

object DocFromProtobuf {
  def convert(synth: s.Synthetic, doc: InternalSemanticDoc): STree =
    new DocFromProtobuf(synth)(new SemanticDocument(doc)).stree(synth.tree)
}
final class DocFromProtobuf(original: s.Synthetic)(
    implicit doc: SemanticDocument) {
  val convert = new SymtabFromProtobuf(doc)
  def stree(t: s.Tree): STree = {
    t match {
      case t: s.ApplyTree =>
        new ApplyTree(t.fn.convert, t.args.convert)
      case t: s.FunctionTree =>
        new FunctionTree(t.params.convert, t.term.convert)
      case t: s.IdTree =>
        sid(t)
      case t: s.LiteralTree =>
        val const = convert.sconstant(t.const)
        new LiteralTree(const)
      case t: s.MacroExpansionTree =>
        val tpe = convert.stype(t.tpe)
        new MacroExpansionTree(t.expandee.convert, tpe)
      case t: s.OriginalTree =>
        soriginal(t.range) match {
          case Some(tree) =>
            val isOriginal = original.range.exists(t.range.contains)
            new OriginalTree(isOriginal, tree)
          case None => NoTree
        }
      case t: s.SelectTree =>
        t.id match {
          case Some(id) =>
            new SelectTree(t.qual.convert, sid(id))
          case None =>
            NoTree
        }
      case t: s.TypeApplyTree =>
        val targs = t.targs.iterator.map(tpe => convert.stype(tpe)).toList
        new TypeApplyTree(t.fn.convert, targs)
      case s.NoTree =>
        NoTree
    }
  }

  private def sid(id: s.IdTree): IdTree =
    new IdTree(Symbol(id.sym))

  private def soriginal(range: Option[s.Range]): Option[Tree] = {
    val pos = ScalametaInternals.positionFromRange(doc.input, range)
    PositionSearch.find(doc.tree, pos)
  }

  private implicit class RichTree(tree: s.Tree) {
    def convert: STree = stree(tree)
  }
  private implicit class RichIds(ids: Seq[s.IdTree]) {
    def convert: List[IdTree] =
      ids.iterator.map(sid).toList
  }
  private implicit class RichTrees(trees: Seq[s.Tree]) {
    def convert: List[STree] = trees.iterator.map(stree).toList
  }

}
