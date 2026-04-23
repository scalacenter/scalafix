package scalafix.internal.v1

import scala.meta.Position
import scala.meta.Term
import scala.meta.Tree

import scalafix.internal.util.PositionSyntax._

object PositionSearch {
  def find(tree: Tree, pos: Position): Option[Tree] = {
    val extrapos = tree match {
      case t: Term.ApplyInfix if t.targClause.values.isEmpty =>
        val lpos = t.lhs.pos
        List(Position.Range(lpos.input, lpos.start, t.op.pos.end))
      case _ =>
        List()
    }
    if (tree.pos == pos || extrapos.contains(pos)) Some(tree)
    else if (tree.pos.contains(pos)) loop(tree.children, pos)
    else None
  }

  private def loop(trees: List[Tree], pos: Position): Option[Tree] = {
    trees match {
      case Nil =>
        None
      case head :: tail =>
        find(head, pos) match {
          case None => loop(tail, pos)
          case x => x
        }
    }
  }
}
