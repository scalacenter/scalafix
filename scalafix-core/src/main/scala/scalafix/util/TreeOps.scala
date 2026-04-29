package scalafix.util

import scala.collection.compat.immutable.LazyList

import scala.meta._
import scala.meta.internal.semanticdb.Scala.Descriptor
import scala.meta.internal.semanticdb.Scala.Symbols

import scalafix.v1.Symbol

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
      case t: Ctor.Primary => Some(t.mods)
      case t: Term.Param => Some(t.mods)
      case t: Type.Param => Some(t.mods)
      case t: Stat.WithMods => Some(t.mods)
      case _ => None
    }
  }
}
object TreeOps {
  def chain(ref: Term.Ref): List[Name] = ref match {
    case n: Name => List(n)
    case Term.Select(qual: Term.Ref, name) => name :: chain(qual)
    case _ => Nil
  }
  def inferGlobalSymbol(tree: Tree): Option[Symbol] = {
    def loop(t: Tree): Option[String] = {
      t match {
        case _: Defn.Val | _: Defn.Var | _: Template =>
          t.parent.flatMap(loop)
        case p: Pkg =>
          for {
            parent <- t.parent
            owner <- loop(parent)
            next = chain(p.ref).foldRight(owner) { case (n, o) =>
              Symbols.Global(o, Descriptor.Package(n.value))
            }
          } yield next
        case d: Member =>
          for {
            parent <- t.parent
            owner <- loop(parent)
            desc <- d match {
              case _: Defn.Object => Some(Descriptor.Term(d.name.value))
              case _: Defn.Class => Some(Descriptor.Type(d.name.value))
              case _: Defn.Trait => Some(Descriptor.Type(d.name.value))
              case _: Pat.Var => Some(Descriptor.Term(d.name.value))
              case _: Defn.Def =>
                for {
                  stats <- parent match {
                    case t: Template => Some(t.body.stats)
                    case _ => None
                  }
                  conflicts = stats.collect {
                    case m: Defn.Def if m.name.value == d.name.value => m
                  }
                  indexOf = conflicts.indexOf(d)
                  disambiguator = {
                    if (indexOf > 0) "(+" + indexOf + ")"
                    else "()"
                  }
                } yield Descriptor.Method(d.name.value, disambiguator)
              case _ => None
            }
          } yield Symbols.Global(owner, desc)
        case _: Source =>
          Some(Symbols.RootPackage)
        case _ =>
          None
      }
    }
    val result = tree match {
      case _: Name =>
        tree.parent.flatMap(loop)
      case _ => loop(tree)
    }
    result.map(Symbol(_))
  }
  def parents(tree: Tree): LazyList[Tree] =
    tree +: (tree.parent match {
      case Some(x) => parents(x)
      case _ => LazyList.empty
    })

  private[scalafix] def traverseTree(f: Tree => Unit)(tree: Tree): Unit = {
    val buf = new collection.mutable.Queue[Tree]
    buf += tree
    while (buf.nonEmpty) {
      val elem = buf.dequeue()
      f(elem)
      elem.children.foreach(buf.+=)
    }
  }

  private[scalafix] def collectTree[A](f: PartialFunction[Tree, A])(
      tree: Tree
  ): List[A] = {
    val buf = List.newBuilder[A]
    val func = f.andThen(buf += _)
    traverseTree(x => func.applyOrElse(x, (_: Tree) => ()))(tree)
    buf.result()
  }

}
