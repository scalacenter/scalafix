package scalafix.internal.v1

import scala.meta.Position
import scala.meta.Term
import scala.meta.Tree
import scala.meta.Type

import scalafix.internal.util.PositionSyntax._

object PositionSearch {
  def find(tree: Tree, pos: Position): Option[Tree] = {
    val extrapos = tree match {
      case Term.ApplyInfix.After_4_6_0(lhs, op, Type.ArgClause(Nil), _) =>
        List(Position.Range(lhs.pos.input, lhs.pos.start, op.pos.end))
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
