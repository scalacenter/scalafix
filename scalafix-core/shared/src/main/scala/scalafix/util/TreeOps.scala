package scalafix.util

import scala.meta._

object TreeExtractors {
  object :&&: {
    def unapply[A](arg: A): Option[(A, A)] =
      Some(arg -> arg)
  }

  object Select {
    def unapply(tree: Tree): Option[(Term, Name)] = tree match {
      case Term.Select(qual, name) => Some(qual -> name)
      case Type.Select(qual, name) => Some(qual -> name)
      case _ => None
    }
  }
}
object TreeOps {
  def parents(tree: Tree): Stream[Tree] =
    tree #:: (tree.parent match {
      case Some(x) => parents(x)
      case _ => Stream.empty
    })
}
