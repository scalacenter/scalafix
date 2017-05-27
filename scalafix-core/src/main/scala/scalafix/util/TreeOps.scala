package scalafix.util

import scala.meta.Tree

object TreeOps {
  def parents(tree: Tree): Stream[Tree] =
    tree #:: (tree.parent match {
      case Some(x) => parents(x)
      case _ => Stream.empty
    })
}
