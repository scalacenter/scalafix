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

  object Mods {
    def unapply(tree: Tree): Option[List[Mod]] = tree match {
      case Ctor.Primary(mods, _, _) => Some(mods)
      case Ctor.Secondary(mods, _, _, _, _) => Some(mods)
      case Decl.Def(mods, _, _, _, _) => Some(mods)
      case Decl.Type(mods, _, _, _) => Some(mods)
      case Decl.Val(mods, _, _) => Some(mods)
      case Decl.Var(mods, _, _) => Some(mods)
      case Defn.Class(mods, _, _, _, _) => Some(mods)
      case Defn.Def(mods, _, _, _, _, _) => Some(mods)
      case Defn.Macro(mods, _, _, _, _, _) => Some(mods)
      case Defn.Object(mods, _, _) => Some(mods)
      case Defn.Trait(mods, _, _, _, _) => Some(mods)
      case Defn.Type(mods, _, _, _) => Some(mods)
      case Defn.Val(mods, _, _, _) => Some(mods)
      case Defn.Var(mods, _, _, _) => Some(mods)
      case Pkg.Object(mods, _, _) => Some(mods)
      case Term.Param(mods, _, _, _) => Some(mods)
      case Type.Param(mods, _, _, _, _, _) => Some(mods)
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
