package scalafix.util

import scala.annotation.tailrec
import scala.meta.Tree

object TreeOps {

  @tailrec
  final def getParents(tree: Tree, accum: Seq[Tree] = Nil): Seq[Tree] = {
    tree.parent match {
      case Some(parent) => getParents(parent, parent +: accum)
      case _ => accum
    }
  }
}
